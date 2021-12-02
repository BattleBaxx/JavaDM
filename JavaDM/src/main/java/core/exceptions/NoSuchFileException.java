package core.exceptions;

public class NoSuchFileException extends BaseException {
    public NoSuchFileException(String message) {
        super(message);
    }
    public NoSuchFileException(String message, int code) {super(message, code);}
}
