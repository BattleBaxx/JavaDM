package core.exceptions;

public class InvalidStateException extends BaseException {
    public InvalidStateException(String message) {
        super(message);
    }
    public InvalidStateException(String message, int code) {super(message, code);}
}
