package core.exceptions;

public class ConnectionException extends BaseException {
    public ConnectionException(String message) {
        super(message);
    }
    public ConnectionException(String message, int code) {super(message, code);}

}
