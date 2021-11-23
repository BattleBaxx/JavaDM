package api.download;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class DownloadService {
	private List<DownloadDTO> downloadDTOList = new ArrayList <> (Arrays.asList(
	                                         new DownloadDTO("a", "b", 1, 2),
	                                         new DownloadDTO("c", "d", 1, 2),
	                                         new DownloadDTO("e", "f", 1, 2)
	                                         ));
	
	public List<DownloadDTO> getDownloads() {
		return downloadDTOList;
	}
	
	public DownloadDTO getDownload(String id) {
		return downloadDTOList.stream().filter(d -> d.getId().equals(id)).findFirst().get();
	}

	public void addDownload(DownloadDTO download) {
		downloadDTOList.add(download);
	}

	public void pauseDownload(String id, DownloadDTO download) {
		
		for(int i = 0; i < downloadDTOList.size(); i++) {
			DownloadDTO d = downloadDTOList.get(i);
			if(d.getId().equals(id)) {
				downloadDTOList.set(i,  download);
				return;
			}
		}
		
	}

	public void deleteDownload(String id) {
		// TODO Auto-generated method stub
		
	}
}
