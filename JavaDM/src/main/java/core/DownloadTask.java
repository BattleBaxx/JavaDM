package core;

import java.util.Map;

public interface DownloadTask {
    void start();
    void cancel();
    void pause();
    void resume();
    DownloadStatus getStatus();
    Map<String, String> getDownloadDetails();
}