package api.file;

import core.DownloadManager;
import core.exceptions.FileException;
import core.exceptions.NoSuchFileException;
import core.util.FileUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FileService {

    private final DownloadManager dm = DownloadManager.getInstance();
    private static final DateTimeFormatter DATE_FORMATTER_WITH_TIME = DateTimeFormatter
            .ofPattern("MMM d, yyyy HH:mm:ss");

    public Map<String, String> getFile(String id) {
        HashMap<String, String> fileDetails = new HashMap<>();

        File requestedFile = new File(FileUtils.getFullFilePath(id));
        if (!requestedFile.canRead()) {
            throw new NoSuchFileException("Invalid ID, file does not exist");
        }

        Path filepath = requestedFile.toPath();
        BasicFileAttributes attr;
        try {
            attr = Files.readAttributes(filepath, BasicFileAttributes.class);
        } catch (IOException e) {
            throw new FileException("An error occurred while reading the attributes of the requested file");
        }
        fileDetails.put("name", id);
        fileDetails.put("created_time", attr.creationTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(DATE_FORMATTER_WITH_TIME));
        fileDetails.put("size", String.valueOf(attr.size()));
        return fileDetails;
    }

    public List<Map<String, String>> getFiles() {
        List<Map<String, String>> files = new ArrayList<>();
        File directoryPath = new File(System.getenv("downloadFolder"));
        String[] contents = directoryPath.list();
        if(contents == null) {
            return null;
        }
        for (String file : contents) {
            files.add(getFile(file));
        }
        return files;
    }

    public void deleteFile(String id) {
        String fullFilePath = FileUtils.getFullFilePath(id);
        File file = new File(fullFilePath);
        if (!file.canRead()) {
            throw new NoSuchFileException("Invalid ID, file does not exist");
        }
        if(!file.delete()) {
            throw new FileException("The file could not be deleted", 500);
        }
        dm.removeDownloadId(id);
    }

    public void updateFile(Map<String, String> updatedFileDetails, String id) {
        File oldFile = new File(FileUtils.getFullFilePath(id));
        if (!oldFile.canRead()) {
            throw new NoSuchFileException("Invalid ID, file does not exist");
        }
        String newName = updatedFileDetails.get("name");
        File newFile = new File(FileUtils.getFullFilePath(newName));
        if (!oldFile.renameTo(newFile)) {
            throw new NoSuchFileException("The file could not be renamed", 500);
        }
        dm.updateDownloadId(id, newName);
    }

    public ResponseEntity<Resource> downloadFile(String id) {
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
}
