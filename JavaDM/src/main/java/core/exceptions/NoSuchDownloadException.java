package core.exceptions;

public class NoSuchDownloadException extends BaseException {
    public NoSuchDownloadException(String message) {
        super(message);
    }
    public NoSuchDownloadException(String message, int code) {super(message, code);}
}
