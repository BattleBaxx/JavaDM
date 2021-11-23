package core;

import core.DownloadTask;
import core.ParallelDownloadTask;
import core.SimpleDownloadTask;
import core.exceptions.InvalidUrlException;
import core.util.HttpUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class DownloadManager {

    private static final DownloadManager dm = new DownloadManager();

    Map<String, DownloadTask> downloadTaskList;
    String downloadFolder;

    private DownloadManager() {
        this.downloadFolder = System.getenv("downloadFolder");
        downloadTaskList = new HashMap<>();
    }

    public static DownloadManager getInstance() {
        return dm;
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

    // If parallel count not specified, it is assumed to be a simple download task

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

    public Map<String, String> getDownloadDetail(String fileName) {
        DownloadTask dt = this.downloadTaskList.get(fileName);
        return dt.getDownloadDetails();
    }

    public List<Map<String, String>> getAllDownloadDetails() {
        List<Map<String, String>> result = new ArrayList<>();
        for(Map.Entry<String, DownloadTask> entry: this.downloadTaskList.entrySet()) {
            DownloadTask dt = entry.getValue();
            result.add(dt.getDownloadDetails());
        }
        return result;
    }
}
