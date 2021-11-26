package core.util;

import java.nio.file.Paths;

public class FileUtils {
    public static String getFullFilePath(String fileName) {
        return Paths.get(System.getenv("downloadFolder"), fileName).toString();
    }
}
