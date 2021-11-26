package api.download;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonView;


@RestController
public class DownloadController {
	
	@Autowired
	private DownloadService downloadService;
	
	@RequestMapping(method = RequestMethod.GET, value ="/downloads")
	public List<Map<String, String>> getAllDownloads() {
		return downloadService.getDownloads();
	}
	
	@RequestMapping(method = RequestMethod.GET, value ="/download/{id}")
	public Map<String, String> getDownload(@PathVariable String id) {
		return downloadService.getDownload(id);
	}
	
	@RequestMapping(method=RequestMethod.POST, value="/download")
	public ResponseEntity<Map<String, String>> addDownload(@RequestBody Map<String, String> download) {
		if(!download.containsKey("url") || !download.containsKey("file_name")) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		return downloadService.addDownload(download);
	}
	
	@RequestMapping(method=RequestMethod.PUT, value="/download/pause/{id}")
	public ResponseEntity<Map<String, String>> pauseDownload(@PathVariable String id) {
		downloadService.pauseDownload(id);
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@RequestMapping(method=RequestMethod.PUT, value="/download/resume/{id}")
	public void resumeDownload(@PathVariable String id) {
		downloadService.resumeDownload(id);
	}
	
	@RequestMapping(method=RequestMethod.DELETE, value="/download/{id}")
	public void deleteDownload(@PathVariable String id) {
		downloadService.cancelDownload(id);
	}
}
