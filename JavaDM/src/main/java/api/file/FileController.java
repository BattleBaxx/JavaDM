package api.file;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        return fileService.downloadFile(id);
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/file/{id}")
    public void updateFile(@RequestBody Map<String, String> updatedFileDetails, @PathVariable String id) {
        fileService.updateFile(updatedFileDetails, id);
    }

    @RequestMapping(method = RequestMethod.DELETE, value = "/file/{id}")
    public void deleteFile(@PathVariable String id) {
        fileService.deleteFile(id);
    }


}
