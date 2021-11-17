package api.download;

import java.util.List;

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
	public void addDownload(@RequestBody DownloadDTO download) {
		downloadService.addDownload(download);
	}
}
