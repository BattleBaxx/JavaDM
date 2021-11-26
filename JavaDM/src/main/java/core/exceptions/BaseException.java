package core.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

public class BaseException extends RuntimeException {
    private int responseCode;

    public BaseException (String message) {
        super(message);
        responseCode = 400;
    }

    public BaseException(String message, int code) {
        super(message);
        responseCode = code;
    }

    public ResponseEntity<Map<String, String>> getResponse() {
        Map<String, String> resp = new HashMap<>();
        resp.put("error", this.getMessage());
        return new ResponseEntity<>(resp, HttpStatus.resolve(responseCode));
    }
}
