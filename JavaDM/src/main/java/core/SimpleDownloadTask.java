package core;

import core.util.HttpUtils;
import okhttp3.Response;

import java.io.*;

class SimpleDownloadUnit implements Runnable {

    private String downloadUrl;
    private int bufferSize;
    private File file;
    private volatile boolean pauseDownload;
    private volatile boolean cancelDownload;
    private long totalDownloadLength;
    private long downloadedLength;
    private DownloadStatus status;

    public SimpleDownloadUnit(String downloadUrl, int bufferSize, File file) throws IOException {
        this.downloadUrl = downloadUrl;
        this.bufferSize = bufferSize;
        this.file = file;
        this.pauseDownload = false;
        this.cancelDownload = false;
        this.status = DownloadStatus.CREATED;

        this.downloadedLength = 0;
        Response serverResponse = HttpUtils.getResponse(this.downloadUrl, "HEAD");
        try {
            this.totalDownloadLength = Long.parseLong(serverResponse.header("Content-Length"));
        } catch(NumberFormatException ne) {
            this.totalDownloadLength = -1;
        }
        System.out.println("Download length total: " + this.totalDownloadLength);
    }

    public void run() {
        if(this.status == DownloadStatus.CANCELLED) {
            throw new IllegalThreadStateException("Task has been cancelled");
        }
        Response serverResponse = null;
        InputStream responseStream = null;
        boolean fileAppendMode = false;
        try {
            serverResponse = HttpUtils.getResponse(this.downloadUrl, "GET");
            responseStream = serverResponse.body().byteStream();

            if(this.status == DownloadStatus.PAUSED) {
                responseStream.skip(this.downloadedLength);
                System.out.println("Resuming download");
                fileAppendMode = true;
            }
        } catch (IOException e) {
            System.out.println("Response / skip error");
        }

        this.status = DownloadStatus.DOWNLOADING;
        try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file, fileAppendMode))) {
            byte[] buffer = new byte[this.bufferSize];

            int bytesReceived;
            int count = 0;

            System.out.println("Before the loop");
            while(true) {
                if(this.cancelDownload) {
                    responseStream.close();
                    outputStream.close();
                    this.status = DownloadStatus.CANCELLED;
                    this.cancelDownload = false;
                    boolean done = this.file.delete();
                    return;
                }

                if(this.pauseDownload) {
                    responseStream.close();
                    outputStream.close();
                    this.status = DownloadStatus.PAUSED;
                    this.pauseDownload = false;
                    return;
                }

                bytesReceived = responseStream.read(buffer);
                if(bytesReceived == -1) break;
                this.downloadedLength += bytesReceived;
                outputStream.write(buffer, 0, bytesReceived);
                outputStream.flush();
                ++count;
                System.out.println("Count: " + count + " Wrote " + bytesReceived);
            }

            serverResponse.close();
        } catch (IOException e) {
            System.out.println("I/O exception");
            e.printStackTrace();
        }
        System.out.println("Download complete");
    }

    public void cancel() {
        this.cancelDownload = true;
//        System.out.println("Download cancelled");
    }

    public void pause() {
        this.pauseDownload = true;
//        System.out.println("Download paused");
//        System.out.println(this.downloadedLength + " out of " + this.totalDownloadLength);
    }

    public DownloadStatus getStatus() {
        return this.status;
    }
}

public class SimpleDownloadTask implements DownloadTask {
    private SimpleDownloadUnit downloadUnit;

    public SimpleDownloadTask(String downloadUrl, int bufferSize, File file) throws IOException {
        downloadUnit = new SimpleDownloadUnit(downloadUrl, bufferSize, file);
    }

    @Override
    public void start() {
        Thread downloadThread = new Thread(downloadUnit);
        downloadThread.start();
    }

    @Override
    public void cancel() {
        downloadUnit.cancel();
    }

    @Override
    public void pause() {
        downloadUnit.pause();
    }

    @Override
    public void resume() {
        Thread downloadThread = new Thread(downloadUnit);
        downloadThread.start();
    }

    @Override
    public DownloadStatus getStatus() {
        return downloadUnit.getStatus();
    }
}