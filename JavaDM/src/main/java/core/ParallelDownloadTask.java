package core;

import core.exceptions.InvalidResponseException;
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

    public ParallelDownloadUnit(String downloadUrl, int bufferSize, long start, long end,  File downloadFile) throws IOException {
        this.downloadUrl = downloadUrl;
        this.bufferSize = bufferSize;
        this.start = start;
        this.end = end;
        this.pauseDownload = false;
        this.cancelDownload = false;
        this.downloadedLength = 0;
        this.status = DownloadStatus.CREATED;

        this.randomAccessFile = new RandomAccessFile(downloadFile, "rw");

        this.randomAccessFile.seek(start);
    }
    @Override
    public void run() {
        if(this.status == DownloadStatus.CANCELLED) {
            throw new IllegalThreadStateException("Task has been cancelled");
        }
        Response serverResponse = null;
        InputStream responseStream = null;
        try {
            OkHttpClient client = HttpClient.getInstance();

            Request partialRequest = new Request.Builder()
                    .url(this.downloadUrl)
                    .header("Range", String.format("bytes=%s-%s", start, end))
                    .get()
                    .build();

            serverResponse = client.newCall(partialRequest).execute();
//            System.out.println("Code: " + serverResponse.code());
            if(serverResponse.code() != 206)
                throw new IOException("Accept - ranges not supported");

            responseStream = serverResponse.body().byteStream();

            if(this.status == DownloadStatus.PAUSED) {
                responseStream.skip(this.downloadedLength);
                System.out.println("Resuming download");
            }
        } catch (IOException e) {
            System.out.println("Response / skip error");
        }

        this.status = DownloadStatus.DOWNLOADING;
        try {
            byte[] buffer = new byte[this.bufferSize];

            int bytesReceived;
            int count = 0;

//            System.out.println("Before the loop");
            while(true) {
                if(this.cancelDownload) {
                    responseStream.close();
                    this.status = DownloadStatus.CANCELLED;
                    this.cancelDownload = false;
                    return;
                }

                if(this.pauseDownload) {
                    responseStream.close();
                    this.status = DownloadStatus.PAUSED;
                    this.pauseDownload = false;
                    return;
                }

                bytesReceived = responseStream.read(buffer);
                if(bytesReceived == -1) break;
                this.downloadedLength += bytesReceived;
                randomAccessFile.write(buffer, 0, bytesReceived);
                ++count;
//                System.out.println("Thread: " + Thread.currentThread().getName() + "Count: " + count + " Wrote " + bytesReceived);
            }

            serverResponse.close();
        } catch (IOException e) {
            System.out.println("I/O exception");
            e.printStackTrace();
        }

        System.out.println("Set to completed");
        this.status = DownloadStatus.COMPLETED;
    }

    public void pause() {
        this.pauseDownload = true;
    }

    public void cancel() {
        this.cancelDownload = true;
    }

    public DownloadStatus getStatus() { return this.status; }

    public long getDownloadedLength() { return this.downloadedLength; }

    public String getDownloadUrl() { return this.downloadUrl; }
}

public class ParallelDownloadTask implements DownloadTask {
    private List<ParallelDownloadUnit> downloadUnits;
    private long totalDownloadLength;
    private long rangeSize;
    private File downloadFile;

    public ParallelDownloadTask(String downloadUrl, int bufferSize, File file, int parallelCount) throws IOException, InvalidResponseException {
        Response serverResponse = HttpUtils.getResponse(downloadUrl, "HEAD");
        try{
            this.totalDownloadLength = Long.parseLong(serverResponse.header("Content-Length"));
        } catch (NumberFormatException ne) {
            throw new InvalidResponseException("Server does not support content-length");
        }

        this.rangeSize = (long) Math.ceil((double) this.totalDownloadLength / parallelCount);
        long offset = 0;


        FileOutputStream fout = new FileOutputStream(file);
        byte buf[] = new byte[1024];
        for (int size = 0; size < this.totalDownloadLength; size += buf.length) {
            fout.write(buf);
            fout.flush();
        }
        fout.close();

        this.downloadFile = file;

        downloadUnits = new ArrayList<>();

        for(int i = 0; i < parallelCount; i++) {
            downloadUnits.add(new ParallelDownloadUnit(downloadUrl, bufferSize, offset, Math.min(offset + this.rangeSize, this.totalDownloadLength), this.downloadFile));
            offset += this.rangeSize;
        }

    }

    @Override
    public void start() {
        DownloadStatus curStatus = this.getStatus();
        if(curStatus == DownloadStatus.PAUSED) {
            throw new IllegalThreadStateException("Thread cannot be started from paused state");
        }

        if(curStatus == DownloadStatus.CANCELLED) {
            throw new IllegalThreadStateException("Thread cannot be started from cancelled state");
        }

        int i = 0;
        for(ParallelDownloadUnit pdu: this.downloadUnits) {
            new Thread(pdu, "Thread " + i).start();
            ++i;
        }
    }

    @Override
    public void cancel() {
        DownloadStatus curStatus = this.getStatus();
        if(curStatus == DownloadStatus.CANCELLED) {
           return;
        }
        if(curStatus == DownloadStatus.COMPLETED) {
            return;
        }

        for(ParallelDownloadUnit pdu: this.downloadUnits) {
            pdu.cancel();
        }

        this.downloadFile.delete();

    }

    @Override
    public void pause() {
        DownloadStatus curStatus = this.getStatus();
        if( curStatus == DownloadStatus.CANCELLED) {
            throw new IllegalThreadStateException("Thread cannot be paused from cancelled state");
        }
        if(curStatus == DownloadStatus.COMPLETED) {
            return;
        }


        for(ParallelDownloadUnit pdu: this.downloadUnits) {
            pdu.pause();
        }
    }

    @Override
    public void resume() {
        DownloadStatus curStatus = this.getStatus();
        if(curStatus == DownloadStatus.CANCELLED) {
            throw new IllegalThreadStateException("Thread cannot be resumed from cancelled state");
        }
        if(curStatus == DownloadStatus.COMPLETED) {
            return;
        }


        int i = 0;
        for(ParallelDownloadUnit pdu: this.downloadUnits) {
            if(pdu.getStatus() == DownloadStatus.PAUSED) {
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

        for(ParallelDownloadUnit pdu: this.downloadUnits) {
            System.out.println(pdu.getStatus());
            if(pdu.getStatus() == DownloadStatus.CANCELLED)
                cancelledCount++;
            if(pdu.getStatus() == DownloadStatus.PAUSED)
                pausedCount++;
            if(pdu.getStatus() == DownloadStatus.CREATED)
                createdCount++;
            if(pdu.getStatus() == DownloadStatus.DOWNLOADING)
                downloadingCount++;
        }

        if(cancelledCount != 0)
            return DownloadStatus.CANCELLED;
        if(createdCount != 0)
            return DownloadStatus.CREATED;
        if(pausedCount != 0)
            return DownloadStatus.PAUSED;
        if(downloadingCount != 0)
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
        for(ParallelDownloadUnit pdu: this.downloadUnits) {
            downloadedLength += pdu.getDownloadedLength();
        }

        if(status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PAUSED) {
            details.put("completedSize", String.valueOf(downloadedLength));
            details.put("totalSize", String.valueOf(this.totalDownloadLength));
        }

        return details;
    }
}
