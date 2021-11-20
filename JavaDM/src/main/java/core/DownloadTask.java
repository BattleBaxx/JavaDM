package core;

public interface DownloadTask {
    void start();
    void cancel();
    void pause();
    void resume();
    DownloadStatus getStatus();
}