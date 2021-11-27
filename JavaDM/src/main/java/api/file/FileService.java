package api.file;

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

    private static final DateTimeFormatter DATE_FORMATTER_WITH_TIME = DateTimeFormatter
            .ofPattern("MMM d, yyyy HH:mm:ss.SSS");
    final private String downloadPath = System.getenv("downloadFolder");

    public Map<String, String> getFile(String id) {
        HashMap<String, String> fileDetails = new HashMap<>();
        Path filepath = new File(downloadPath + id).toPath();
        BasicFileAttributes attr = null;
        try {
            attr = Files.readAttributes(filepath, BasicFileAttributes.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        fileDetails.put("name", id);
        fileDetails.put("created_time", attr.creationTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(DATE_FORMATTER_WITH_TIME));
        fileDetails.put("size", String.valueOf(attr.size()));
        return fileDetails;
    }

    public List<Map<String, String>> getFiles() {
        List<Map<String, String>> files = new ArrayList<>();
        File directoryPath = new File(downloadPath);
        String contents[] = directoryPath.list();
        for (String file : contents) {
            files.add(getFile(file));
        }
        return files;
    }

    public void deleteFile(String id) {
        File file = new File(downloadPath + id);
        file.delete();
    }

    public void updateFile(Map<String, Object> updatedFileDetails, String id) {
        File oldFile = new File(downloadPath + id);
        File newFile = new File(downloadPath + updatedFileDetails.get("name"));
        oldFile.renameTo(newFile);
    }
}
