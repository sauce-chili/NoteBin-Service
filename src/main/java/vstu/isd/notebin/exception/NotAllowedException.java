package vstu.isd.notebin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public class NotAllowedException extends BaseClientException {
    public NotAllowedException(String reason) {
        super(reason, ClientExceptionName.NOT_ALLOWED, HttpStatus.FORBIDDEN);
    }
}
