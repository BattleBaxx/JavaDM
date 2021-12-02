package core;

import core.exceptions.InvalidUrlException;
import core.exceptions.NoSuchDownloadException;
import core.util.FileUtils;
import core.util.HttpUtils;

import java.io.File;
import java.nio.file.Path;
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

    public String createDownloadTask(String url, String fileName) {
        return createDownloadTask(url, 1024, fileName);
    }

    public String createDownloadTask(String url, int maxBufferSize, String fileName) {
        String fullFileName = this.createDownloadTaskInit(url, fileName);

        String fullFilePath = FileUtils.getFullFilePath(fullFileName);
        File downloadFile = new File(fullFilePath);

        DownloadTask newTask = new SerialDownloadTask(url, maxBufferSize, downloadFile);
        this.downloadTaskList.put(fullFileName, newTask);
        newTask.start();

        return fullFileName;
    }

    // If parallel count not specified, it is assumed to be a simple download task

    public String createDownloadTask(String url, String fileName, int parallelCount) {
        return createDownloadTask(url, 1024, fileName, parallelCount);
    }

    public String createDownloadTask(String url, int maxBufferSize, String fileName, int parallelCount) {
        String fullFileName = this.createDownloadTaskInit(url, fileName);

        String fullFilePath = Paths.get(System.getenv("downloadFolder"), fullFileName).toString();
        File downloadFile = new File(fullFilePath);

        DownloadTask newTask = new ParallelDownloadTask(url, maxBufferSize, downloadFile, parallelCount);
        this.downloadTaskList.put(fullFileName, newTask);
        newTask.start();

        return fullFileName;
    }

    private String createDownloadTaskInit(String url, String fileName) {
        if(!HttpUtils.validUrl(url)) {
            throw new InvalidUrlException("The provided URL is invalid");
        }


        String fileExtension = HttpUtils.getExtension(url);
        String fileNameExtension = fileName + "." + fileExtension;

        if(this.downloadTaskList.containsKey(fileNameExtension)) {
            throw new NoSuchDownloadException("Download ID already exists");
        }
        return fileNameExtension;
    }

    public void pauseDownload(String fileName) {
        if(!this.downloadTaskList.containsKey(fileName)) {
            throw new NoSuchDownloadException("Invalid download ID");
        }
        this.downloadTaskList.get(fileName).pause();
    }

    public void cancelDownload(String fileName) {
        if(!this.downloadTaskList.containsKey(fileName)) {
            throw new NoSuchDownloadException("Invalid download ID");
        }
        this.downloadTaskList.get(fileName).cancel();
    }

    public void resumeDownload(String fileName) {
        if(!this.downloadTaskList.containsKey(fileName)) {
            throw new NoSuchDownloadException("Invalid download ID");
        }
        this.downloadTaskList.get(fileName).resume();
    }

    public void getDownloadStatus(String fileName) {
        if(!this.downloadTaskList.containsKey(fileName)) {
            throw new NoSuchDownloadException("Invalid download ID");
        }
        this.downloadTaskList.get(fileName).getStatus();
    }

    public Map<String, String> getDownloadDetail(String fileName) {
        if(!this.downloadTaskList.containsKey(fileName)) {
            throw new NoSuchDownloadException("Invalid download ID");
        }
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

    public void updateDownloadId(String existingId, String newId) {
        if(!downloadTaskList.containsKey(existingId)) {
            throw new NoSuchDownloadException("Provided download ID does not exist");
        }
        DownloadTask dt = this.downloadTaskList.get(existingId);
        this.downloadTaskList.put(newId, dt);
        this.downloadTaskList.remove(existingId);
    }

    public void removeDownloadId(String id) {
        if(!downloadTaskList.containsKey(id)) {
            throw new NoSuchDownloadException("Provided download ID does not exist");
        }
        this.downloadTaskList.remove(id);
    }
}
