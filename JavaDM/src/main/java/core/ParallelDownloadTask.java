package core;

import core.exceptions.InvalidResponseException;
import core.util.HttpUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.*;
import java.util.ArrayList;
import java.util.List;


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
            System.out.println("Code: " + serverResponse.code());
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

            System.out.println("Before the loop");
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
                System.out.println("Thread: " + Thread.currentThread().getName() + "Count: " + count + " Wrote " + bytesReceived);
            }

            serverResponse.close();
        } catch (IOException e) {
            System.out.println("I/O exception");
            e.printStackTrace();
        }
        System.out.println("Unit complete");
    }

    public void pause() {
        this.pauseDownload = true;
    }

    public void cancel() {
        this.cancelDownload = true;
    }
}

public class ParallelDownloadTask implements DownloadTask {
    private List<ParallelDownloadUnit> downloadUnits;
    private long totalDownloadLength;
    private long rangeSize;
    private File downloadFile;
    private DownloadStatus status;

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

        this.status = DownloadStatus.CREATED;
    }

    @Override
    public void start() {
        if(this.status == DownloadStatus.PAUSED) {
            throw new IllegalThreadStateException("Thread cannot be started from paused state");
        }

        if(this.status == DownloadStatus.CANCELLED) {
            throw new IllegalThreadStateException("Thread cannot be started from cancelled state");
        }

        int i = 0;
        for(ParallelDownloadUnit pdu: this.downloadUnits) {
            new Thread(pdu, "Thread " + i).start();
            ++i;
        }
        this.status = DownloadStatus.DOWNLOADING;
    }

    @Override
    public void cancel() {
        if(this.status == DownloadStatus.CANCELLED) {
           return;
        }

        for(ParallelDownloadUnit pdu: this.downloadUnits) {
            pdu.cancel();
        }

        this.downloadFile.delete();
        this.status = DownloadStatus.CANCELLED;

    }

    @Override
    public void pause() {
        if(this.status == DownloadStatus.CANCELLED) {
            throw new IllegalThreadStateException("Thread cannot be paused from cancelled state");
        }

        for(ParallelDownloadUnit pdu: this.downloadUnits) {
            pdu.pause();
        }
        this.status = DownloadStatus.PAUSED;
    }

    @Override
    public void resume() {
        if(this.status == DownloadStatus.CANCELLED) {
            throw new IllegalThreadStateException("Thread cannot be resumed from cancelled state");
        }

        int i = 0;
        for(ParallelDownloadUnit pdu: this.downloadUnits) {
            new Thread(pdu, "Thread " + i).start();
            ++i;
        }
        this.status = DownloadStatus.DOWNLOADING;
    }

    @Override
    public DownloadStatus getStatus() {
        return this.status;
    }
}
