package core.exceptions;

public class RangesUnsupportedException extends BaseException {
    public RangesUnsupportedException(String message) {
        super(message);
    }
    public RangesUnsupportedException(String message, int code) {super(message, code);}
}
