package core;

import core.DownloadTask;
import core.ParallelDownloadTask;
import core.SimpleDownloadTask;
import core.exceptions.InvalidUrlException;
import core.util.HttpUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class DownloadManager {
    Map<String, DownloadTask> downloadTaskList;
    String downloadFolder;

    public DownloadManager(String downloadFolder) {
        this.downloadFolder = downloadFolder;
        downloadTaskList = new HashMap<>();
    }

    public void createDownloadTask(String url, String fileName) throws InvalidUrlException, IOException {
        createDownloadTask(url, 1024, fileName);
    }

    public void createDownloadTask(String url, int maxBufferSize, String fileName) throws InvalidUrlException, IOException {
        if(!HttpUtils.validUrl(url)) {
            throw new InvalidUrlException("The provided URL is invalid");
        }
        String fileExtension = HttpUtils.getExtension(url);
        String fullFilePath = Paths.get(this.downloadFolder,fileName + "." + fileExtension).toString();
        File downloadFile = new File(fullFilePath);

        DownloadTask newTask = new SimpleDownloadTask(url, maxBufferSize, downloadFile);
        newTask.start();
        downloadTaskList.put(fileName, newTask);
    }

    public void createDownloadTask(String url, String fileName, int parallelCount) throws InvalidUrlException, IOException {
        createDownloadTask(url, 1024, fileName, parallelCount);
    }

    public void createDownloadTask(String url, int maxBufferSize, String fileName, int parallelCount) throws InvalidUrlException, IOException {
        if(!HttpUtils.validUrl(url)) {
            throw new InvalidUrlException("The provided URL is invalid");
        }

        String fileExtension = HttpUtils.getExtension(url);
        String fullFilePath = Paths.get(this.downloadFolder,fileName + "." + fileExtension).toString();
        File downloadFile = new File(fullFilePath);

        DownloadTask newTask = new ParallelDownloadTask(url, maxBufferSize, downloadFile, parallelCount);
        newTask.start();
        downloadTaskList.put(fileName, newTask);
    }

    public void pauseDownload(String fileName) {
        this.downloadTaskList.get(fileName).pause();
    }

    public void cancelDownload(String fileName) {
        this.downloadTaskList.get(fileName).cancel();
    }

    public void resumeDownload(String fileName) {
        this.downloadTaskList.get(fileName).resume();
    }

    public void getDownloadStatus(String fileName) {
        this.downloadTaskList.get(fileName).getStatus();
    }
}
