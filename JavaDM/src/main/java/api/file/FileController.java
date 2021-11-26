package api.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

import core.exceptions.NoSuchFileException;
import core.util.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

	@RequestMapping(method = RequestMethod.GET, value ="/downloadFile/{id}")
	public ResponseEntity<Resource> getFileDownload(@PathVariable String id) {
		String fullFilePath = FileUtils.getFullFilePath(id);

		File respFile = new File(fullFilePath);

		if(!respFile.canRead()) {
			throw new NoSuchFileException("Invalid ID, file does not exist");
		}

		FileSystemResource fsr = new FileSystemResource(respFile);

		return ResponseEntity.ok()
				.contentLength(respFile.length())
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(fsr);
	}
	
	@RequestMapping(method=RequestMethod.PUT, value="/file/{id}")
	public void updateFile(@RequestBody Map<String, Object> file, @PathVariable String id) {
		
	}
	
	@RequestMapping(method=RequestMethod.DELETE, value="/file/{id}")
	public void deleteFile(@PathVariable String id) {
		
	}
	
	
}
