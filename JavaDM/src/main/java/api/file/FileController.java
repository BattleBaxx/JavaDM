package api.file;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FileController {
	
	@Autowired
	private FileService fileService;
	
	@RequestMapping("/files")
	public void getAllFiles() {
		
	}
	
	@RequestMapping("/file/{id}")
	public void getFile(@PathVariable String id) {
		
	}
	
	@RequestMapping("/downloadFile/{id}")
	public void downloadFile(@PathVariable String id) {
		
	}
	
	@RequestMapping(method=RequestMethod.PUT, value="/file/{id}")
	public void updateFile(@RequestBody Map<String, Object> file, @PathVariable String id) {
		
	}
	
	@RequestMapping(method=RequestMethod.DELETE, value="/file/{id}")
	public void deleteFile(@PathVariable String id) {
		
	}
	
	
}
