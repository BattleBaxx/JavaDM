package core.exceptions;

public class InvalidUrlException extends BaseException {
    public InvalidUrlException(String message) {
        super(message);
    }
    public InvalidUrlException(String message, int code) {super(message, code);}
}
