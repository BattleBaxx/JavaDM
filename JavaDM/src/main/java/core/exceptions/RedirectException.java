package core.exceptions;

public class RedirectException extends BaseException {
    public RedirectException(String message) {
        super(message);
    }
    public RedirectException(String message, int code) {super(message, code);}
}