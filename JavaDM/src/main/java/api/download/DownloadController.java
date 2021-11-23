package api.download;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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
	
	@RequestMapping("/downloads")
	@JsonView(View.Get.class)
	public List<DownloadDTO> getAllDownloads() {
		return downloadService.getDownloads();
	}
	
	@RequestMapping("/download/{id}")
	@JsonView(View.Get.class)
	public DownloadDTO getDownload(@PathVariable String id) {
		return downloadService.getDownload(id);
	}
	
	@RequestMapping(method=RequestMethod.POST, value="/download")
	public void addDownload(@RequestBody Map<String, Object> download) {
//		downloadService.addDownload(download);
		System.out.println(download);
	}
	
	@RequestMapping(method=RequestMethod.PUT, value="/download/pause/{id}")
	public void pauseDownload(@RequestBody Map<String, Object> download, @PathVariable String id) {
//		downloadService.pauseDownload(id, download);
	}
	
	@RequestMapping(method=RequestMethod.PUT, value="/download/resume/{id}")
	public void resumeDownload(@RequestBody Map<String, Object> download, @PathVariable String id) {
//		downloadService.resumeDownload(id, download);
	}
	
	@RequestMapping(method=RequestMethod.DELETE, value="/download/{id}")
	public void deleteDownload(@PathVariable String id) {
		downloadService.deleteDownload(id);
	}
}
