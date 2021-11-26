package api.download;

import java.util.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import core.DownloadManager;

@Service
public class DownloadService {
    private DownloadManager dm = DownloadManager.getInstance();

    public List<Map<String, String>> getDownloads() {
        return dm.getAllDownloadDetails();
    }

    public Map<String, String> getDownload(String id) {
        return dm.getDownloadDetail(id);
    }

    public ResponseEntity<Map<String, String>> addDownload(Map<String, String> download) {
        String url = download.get("url");
        String fileName = download.get("file_name");
        boolean containsParallel = download.containsKey("parallel_count");
        boolean containsBufferSize = download.containsKey("buffer_size");
        String id;

        if (containsParallel) {
            int parallelCount = Integer.parseInt(download.get("parallel_count"));
            if (containsBufferSize) {
                int bufferSize = Integer.parseInt(download.get("buffer_size"));
                id = dm.createDownloadTask(url, bufferSize, fileName, parallelCount);
            } else {
                id = dm.createDownloadTask(url, fileName, parallelCount);
            }

        } else {
            if (containsBufferSize) {
                int bufferSize = Integer.parseInt(download.get("buffer_size"));
                id = dm.createDownloadTask(url, bufferSize, fileName);
            } else {
                id = dm.createDownloadTask(url, fileName);
            }
        }

        Map<String, String> resp = new HashMap<>();
        resp.put("id", id);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    public void pauseDownload(String id) {
        dm.pauseDownload(id);
    }

    public void resumeDownload(String id) {
        dm.resumeDownload(id);
    }

    public void cancelDownload(String id) {
        dm.cancelDownload(id);

    }
}
