package core.exceptions;

public class InvalidResponseException extends BaseException {
    public InvalidResponseException(String message) {
        super(message);
    }
    public InvalidResponseException(String message, int code) {super(message, code);}

}
