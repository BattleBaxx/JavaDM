package api.download;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import core.DownloadManager;
import core.exceptions.InvalidUrlException;

@Service
public class DownloadService {
	private DownloadManager dm = DownloadManager.getInstance();
	
	public List<Map<String, String>> getDownloads() {
		return dm.getAllDownloadDetails();
	}
	
	public Map<String, String> getDownload(String id) {
		return dm.getDownloadDetail(id);
	}

	public void addDownload(Map<String, String> download) {
//		dm.a
		String url = download.get("url");
		String fileName = download.get("file_name");
		boolean containsParallel = download.containsKey("prallel_count");
		boolean containsBufferSize = download.containsKey("buffer_size");
		
		if(containsParallel) {
			int parallelCount = Integer.parseInt(download.get("parallel_count"));
			if(containsBufferSize) {
				int bufferSize = Integer.parseInt(download.get("buffer_size"));
				try {
					dm.createDownloadTask(url, bufferSize, fileName, parallelCount);
				} catch (InvalidUrlException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else {
				try {
					dm.createDownloadTask(url,fileName, parallelCount);
				} catch (InvalidUrlException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		}else {
			if(containsBufferSize) {
				int bufferSize = Integer.parseInt(download.get("buffer_size"));
				try {
					dm.createDownloadTask(url, bufferSize, fileName);
				} catch (InvalidUrlException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else {
				try {
					dm.createDownloadTask(url,fileName);
				} catch (InvalidUrlException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
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
