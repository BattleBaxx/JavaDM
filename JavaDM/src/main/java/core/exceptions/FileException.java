package core.exceptions;

public class FileException extends BaseException {
    public FileException(String message) {
        super(message);
    }
    public FileException(String message, int code) {super(message, code);}
}
