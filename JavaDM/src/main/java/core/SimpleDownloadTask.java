package core;

import core.util.HttpUtils;
import okhttp3.Response;

import java.io.*;

public class SimpleDownloadTask implements DownloadTask {

    private String downloadUrl;
    private int bufferSize;
    private String filePath;
    private volatile boolean pauseDownload;
    private volatile boolean cancelDownload;
    private long totalDownloadLength;
    private long downloadedLength;
    private DownloadStatus status;

    public SimpleDownloadTask(String downloadUrl, int bufferSize, String filePath) throws IOException {
        this.downloadUrl = downloadUrl;
        this.bufferSize = bufferSize;
        this.filePath = filePath;
        this.pauseDownload = false;
        this.cancelDownload = false;
        this.status = DownloadStatus.CREATED;

        this.downloadedLength = 0;
        Response serverResponse = HttpUtils.getResponse(this.downloadUrl, "HEAD");
        try {
            this.totalDownloadLength = Long.parseLong(serverResponse.header("Content-Length"));
        } catch(NumberFormatException ne) {
            System.out.println("No content-length from server");
        }
        System.out.println("Download length total: " + this.totalDownloadLength);
    }

    @Override
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
        try (BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(filePath, fileAppendMode))) {
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
                    boolean done = new File(this.filePath).delete();
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

    @Override
    public void cancel() {
        this.cancelDownload = true;
        System.out.println("Download cancelled");
    }

    @Override
    public void pause() {
        this.pauseDownload = true;
        System.out.println("Download paused");
        System.out.println(this.downloadedLength + " out of " + this.totalDownloadLength);
    }

    @Override
    public DownloadStatus getStatus() {
        return this.status;
    }

}
