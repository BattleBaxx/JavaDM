package core;

public enum DownloadStatus {

    CREATED(1), DOWNLOADING(2), PAUSED(3), COMPLETED(4), CANCELLED(5);

    private int statusValue;

    private DownloadStatus(int statusValue) {
        this.statusValue = statusValue;
    }

}
