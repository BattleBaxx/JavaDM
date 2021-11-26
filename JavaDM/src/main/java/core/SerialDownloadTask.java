package core;

import core.exceptions.ConnectionException;
import core.exceptions.FileException;
import core.exceptions.InvalidStateException;
import core.util.HttpUtils;
import okhttp3.Response;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

class SerialDownloadUnit implements Runnable {

    private String downloadUrl;
    private int bufferSize;
    private File file;
    private volatile boolean pauseDownload;
    private volatile boolean cancelDownload;
    private long totalDownloadLength;
    private long downloadedLength;
    private DownloadStatus status;

    public SerialDownloadUnit(String downloadUrl, int bufferSize, File file) {
        this.downloadUrl = downloadUrl;
        this.bufferSize = bufferSize;
        this.file = file;
        this.pauseDownload = false;
        this.cancelDownload = false;
        this.status = DownloadStatus.CREATED;

        this.downloadedLength = 0;
        Response serverResponse;
        serverResponse = HttpUtils.getResponse(this.downloadUrl, "HEAD");

        try {
            this.totalDownloadLength = Long.parseLong(serverResponse.header("Content-Length"));
        } catch(NumberFormatException ne) {
            this.totalDownloadLength = -1;
        }
        System.out.println("Download length total: " + this.totalDownloadLength);
    }

    public void run() {
        if(this.status == DownloadStatus.CANCELLED) {
            throw new InvalidStateException("A cancelled download cannot be restarted");
        }
        Response serverResponse = null;
        InputStream responseStream = null;
        boolean fileAppendMode = false;

        serverResponse = HttpUtils.getResponse(this.downloadUrl, "GET");
        responseStream = serverResponse.body().byteStream();

        try {
            if(this.status == DownloadStatus.PAUSED) {
                responseStream.skip(this.downloadedLength);
                System.out.println("Resuming download");
                fileAppendMode = true;
            }
        } catch (IOException e) {
            throw new ConnectionException("Error in resuming download");
        }

        this.status = DownloadStatus.DOWNLOADING;

        BufferedOutputStream outputStream;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(file, fileAppendMode));
        } catch(FileNotFoundException fe) {
            throw new FileException("File could not be opened for writing");
        }

        byte[] buffer = new byte[this.bufferSize];

        int bytesReceived;
        int count = 0;

        System.out.println("Before the loop");
        while(true) {
            if(this.cancelDownload) {
                try {
                    responseStream.close();
                } catch (IOException e) {
                    throw new ConnectionException("Exception while closing server stream");
                }
                try {
                    outputStream.close();
                } catch (IOException e) {
                    throw new FileException("Exception while closing file stream");
                }
                this.status = DownloadStatus.CANCELLED;
                this.cancelDownload = false;
                this.file.delete();
                return;
            }

            if(this.pauseDownload) {
                try {
                    responseStream.close();
                } catch (IOException e) {
                    throw new ConnectionException("Exception while closing server stream");
                }
                try {
                    outputStream.close();
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
                e.printStackTrace();
                throw new ConnectionException("Exception while reading from server response stream");
            }

            if(bytesReceived == -1) break;
            this.downloadedLength += bytesReceived;

            try {
                outputStream.write(buffer, 0, bytesReceived);
                outputStream.flush();
            } catch (IOException e) {
                throw new FileException("Exception while writing to file");
            }

            ++count;
            System.out.println("Count: " + count + " Wrote " + bytesReceived);


        }

        serverResponse.close();
        this.status = DownloadStatus.COMPLETED;
        System.out.println("Download complete");
    }

    public void cancel() {
        if(this.status == DownloadStatus.COMPLETED) {
            throw new InvalidStateException("An already completed download cannot be cancelled");
        } else {
            this.cancelDownload = true;
        }

    }

    public void pause() {
        if(this.status == DownloadStatus.DOWNLOADING) {
            this.pauseDownload = true;
        } else {
            throw new InvalidStateException(String.format("Download in the %s state cannot be paused", this.status));
        }
    }

    public DownloadStatus getStatus() {
        return this.status;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public long getTotalDownloadLength() {
        return totalDownloadLength;
    }

    public long getDownloadedLength() {
        return downloadedLength;
    }
}

public class SerialDownloadTask implements DownloadTask {
    private SerialDownloadUnit downloadUnit;

    public SerialDownloadTask(String downloadUrl, int bufferSize, File file) {
        downloadUnit = new SerialDownloadUnit(downloadUrl, bufferSize, file);
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

    @Override
    public Map<String, String> getDownloadDetails() {
        Map<String, String> details = new HashMap<>();

        String url = this.downloadUnit.getDownloadUrl();
        details.put("url", url);

        DownloadStatus status = this.downloadUnit.getStatus();
        details.put("status", status.name());

        if(status == DownloadStatus.DOWNLOADING || status == DownloadStatus.PAUSED) {
            details.put("completedSize", String.valueOf(this.downloadUnit.getDownloadedLength()));
            details.put("totalSize", String.valueOf(this.downloadUnit.getTotalDownloadLength()));
        }

        return details;
    }
}