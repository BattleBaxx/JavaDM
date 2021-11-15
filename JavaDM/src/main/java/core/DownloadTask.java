package core;

public interface DownloadTask extends Runnable {
    void cancel();
    void pause();
    DownloadStatus getStatus();
}