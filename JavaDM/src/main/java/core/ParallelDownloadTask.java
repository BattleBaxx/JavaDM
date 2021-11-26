package core;

import core.exceptions.*;
import core.util.HttpUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


class ParallelDownloadUnit implements Runnable {
    private String downloadUrl;
    private int bufferSize;
    private long start;
    private long end;
    private volatile boolean pauseDownload;
    private volatile boolean cancelDownload;
    private long downloadedLength;
    private RandomAccessFile randomAccessFile;
    private DownloadStatus status;

    public ParallelDownloadUnit(String downloadUrl, int bufferSize, long start, long end, File downloadFile) {
        this.downloadUrl = downloadUrl;
        this.bufferSize = bufferSize;
        this.start = start;
        this.end = end;
        this.pauseDownload = false;
        this.cancelDownload = false;
        this.downloadedLength = 0;
        this.status = DownloadStatus.CREATED;

        try {
            this.randomAccessFile = new RandomAccessFile(downloadFile, "rw");
            this.randomAccessFile.seek(start);
        } catch (IOException e) {
            throw new FileException("Error while opening random access file");
        }
    }

    @Override
    public void run() {
        if (this.status == DownloadStatus.CANCELLED) {
            throw new InvalidStateException("Task has been cancelled");
        }
        Response serverResponse = null;
        InputStream responseStream = null;
        OkHttpClient client = HttpClient.getInstance();

        Request partialRequest = new Request.Builder()
                .url(this.downloadUrl)
                .header("Range", String.format("bytes=%s-%s", start, end))
                .get()
                .build();
        try {
            serverResponse = client.newCall(partialRequest).execute();
        } catch (IOException e) {
            throw new ConnectionException("An error occurred while connecting to the server");
        }
//            System.out.println("Code: " + serverResponse.code());
        if (serverResponse.code() != 206)
            throw new RangesUnsupportedException("Accept - ranges not supported");

        responseStream = serverResponse.body().byteStream();

        if (this.status == DownloadStatus.PAUSED) {
            try {
                responseStream.skip(this.downloadedLength);
            } catch (IOException e) {
                throw new ConnectionException("An exception occurred while skipping the response stream");
            }
            System.out.println("Resuming download");
        }

        this.status = DownloadStatus.DOWNLOADING;
        byte[] buffer = new byte[this.bufferSize];

        int bytesReceived;
        int count = 0;

//            System.out.println("Before the loop");
        while (true) {
            if (this.cancelDownload) {
                try {
                    responseStream.close();
                } catch (IOException e) {
                    throw new ConnectionException("Exception while closing server stream");
                }
                try {
                    randomAccessFile.close();
                } catch (IOException e) {
                    throw new FileException("Exception while closing file stream");
                }

                this.status = DownloadStatus.CANCELLED;
                this.cancelDownload = false;
                return;
            }

            if (this.pauseDownload) {
                try {
                    responseStream.close();
                } catch (IOException e) {
                    throw new ConnectionException("Exception while closing server stream");
                }
                try {
                    randomAccessFile.close();
                } catch (IOException e) {
                    throw new FileException("Exception while closing file stream");
                }

                this.status = DownloadStatus.PAUSED;
                this.pauseDownload = false;
                return;
            }

            try {
                bytesReceived = responseStream.read(buffer);
            } catch (IOException e) {
                throw new ConnectionException("Exception while reading from server response stream");
            }
            if (bytesReceived == -1) break;
            this.downloadedLength += bytesReceived;

            try {
                randomAccessFile.write(buffer, 0, bytesReceived);
            } catch (IOException e) {
                throw new FileException("Exception while writing to file");
            }
            ++count;
//                System.out.println("Thread: " + Thread.currentThread().getName() + "Count: " + count + " Wrote " + bytesReceived);
        }

        serverResponse.close();


        System.out.println("Set to completed");
        this.status = DownloadStatus.COMPLETED;
    }

    public void pause() {
        this.pauseDownload = true;
    }

    public void cancel() {
        this.cancelDownload = true;
    }

    public DownloadStatus getStatus() {
        return this.status;
    }

    public long getDownloadedLength() {
        return this.downloadedLength;
    }

    public String getDownloadUrl() {
        return this.downloadUrl;
    }
}

public class ParallelDownloadTask implements DownloadTask {
    private List<ParallelDownloadUnit> downloadUnits;
    private long totalDownloadLength;
    private long rangeSize;
    private File downloadFile;

    public ParallelDownloadTask(String downloadUrl, int bufferSize, File file, int parallelCount) {
        Response serverResponse = HttpUtils.getResponse(downloadUrl, "HEAD");
        try {
            this.totalDownloadLength = Long.parseLong(serverResponse.header("Content-Length"));
        } catch (NumberFormatException ne) {
            throw new InvalidResponseException("Server does not support content-length");
        }

        this.rangeSize = (long) Math.ceil((double) this.totalDownloadLength / parallelCount);
        long offset = 0;

        try {
            FileOutputStream fout = new FileOutputStream(file);
            byte buf[] = new byte[1024];
            for (int size = 0; size < this.totalDownloadLength; size += buf.length) {
                fout.write(buf);
                fout.flush();
            }
            fout.close();
        } catch (IOException e) {
            throw new FileException("Error while creating file");
        }

        this.downloadFile = file;

        downloadUnits = new ArrayList<>();

        for (int i = 0; i < parallelCount; i++) {
            downloadUnits.add(new ParallelDownloadUnit(downloadUrl, bufferSize, offset, Math.min(offset + this.rangeSize, this.totalDownloadLength), this.downloadFile));
            offset += this.rangeSize;
        }

    }

    @Override
    public void start() {
        DownloadStatus curStatus = this.getStatus();

        if (curStatus != DownloadStatus.CREATED) {
            throw new InvalidStateException(String.format("Download cannot be paused from %s state", curStatus));
        }

        int i = 0;
        for (ParallelDownloadUnit pdu : this.downloadUnits) {
            new Thread(pdu, "Thread " + i).start();
            ++i;
        }
    }

    @Override
    public void cancel() {
        DownloadStatus curStatus = this.getStatus();
        if (curStatus == DownloadStatus.CANCELLED) {
            return;
        }
        if (curStatus == DownloadStatus.COMPLETED) {
            throw new InvalidStateException("An already completed download cannot be cancelled");
        }

        for (ParallelDownloadUnit pdu : this.downloadUnits) {
            pdu.cancel();
        }

        this.downloadFile.delete();

    }

    @Override
    public void pause() {
        DownloadStatus curStatus = this.getStatus();

        if (curStatus != DownloadStatus.DOWNLOADING) {
            throw new InvalidStateException(String.format("Download cannot be paused from %s state", curStatus));
        }

        for (ParallelDownloadUnit pdu : this.downloadUnits) {
            if (pdu.getStatus() == DownloadStatus.DOWNLOADING) {
                pdu.pause();
            }
        }
    }

    @Override
    public void resume() {
        DownloadStatus curStatus = this.getStatus();

        if (curStatus != DownloadStatus.PAUSED) {
            throw new InvalidStateException(String.format("Download cannot be resumed from %s state", curStatus));
        }

        int i = 0;
        for (ParallelDownloadUnit pdu : this.downloadUnits) {
            if (pdu.getStatus() == DownloadStatus.PAUSED) {
                new Thread(pdu, "Thread " + i).start();
                ++i;
            }
        }
    }

    @Override
    public DownloadStatus getStatus() {
        int cancelledCount = 0;
        int pausedCount = 0;
        int createdCount = 0;
        int downloadingCount = 0;

        for (ParallelDownloadUnit pdu : this.downloadUnits) {
            System.out.println(pdu.getStatus());
            if (pdu.getStatus() == DownloadStatus.CANCELLED)
                cancelledCount++;
            if (pdu.getStatus() == DownloadStatus.PAUSED)
                pausedCount++;
            if (pdu.getStatus() == DownloadStatus.CREATED)
                createdCount++;
            if (pdu.getStatus() == DownloadStatus.DOWNLOADING)
                downloadingCount++;
        }

        if (cancelledCount != 0)
            return DownloadStatus.CANCELLED;
        if (createdCount != 0)
            return DownloadStatus.CREATED;
        if (pausedCount != 0)
            return DownloadStatus.PAUSED;
        if (downloadingCount != 0)
            return DownloadStatus.DOWNLOADING;

        return DownloadStatus.COMPLETED;
    }

    @Override
    public Map<String, String> getDownloadDetails() {
        Map<String, String> details = new HashMap<>();

        String url = this.downloadUnits.get(0).getDownloadUrl();
        details.put("url", url);

        DownloadStatus status = this.getStatus();
        details.put("status", status.name());

        long downloadedLength = 0;
        for (ParallelDownloadUnit pdu : this.downloadUnits) {
            downloadedLength += pdu.getDownloadedLength();
        }

        if (status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PAUSED) {
            details.put("completedSize", String.valueOf(downloadedLength));
            details.put("totalSize", String.valueOf(this.totalDownloadLength));
        }

        return details;
    }
}
