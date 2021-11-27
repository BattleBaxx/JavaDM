package api.file;

import core.exceptions.NoSuchFileException;
import core.util.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;
import java.util.Map;

@RestController
public class FileController {

    @Autowired
    private FileService fileService;

    @RequestMapping("/files")
    public List<Map<String, String>> getAllFiles() {
        return fileService.getFiles();
    }

    @RequestMapping("/file/{id}")
    public Map<String, String> getFile(@PathVariable String id) {
        return fileService.getFile(id);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/downloadFile/{id}")
    public ResponseEntity<Resource> getFileDownload(@PathVariable String id) {
        String fullFilePath = FileUtils.getFullFilePath(id);

        File respFile = new File(fullFilePath);

        if (!respFile.canRead()) {
            throw new NoSuchFileException("Invalid ID, file does not exist");
        }

        FileSystemResource fsr = new FileSystemResource(respFile);

        return ResponseEntity.ok()
                .contentLength(respFile.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(fsr);
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/file/{id}")
    public void updateFile(@RequestBody Map<String, Object> updatedFileDetails, @PathVariable String id) {
        fileService.updateFile(updatedFileDetails, id);
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/file/{id}")
    public void deleteFile(@PathVariable String id) {
        fileService.deleteFile(id);
    }


}
