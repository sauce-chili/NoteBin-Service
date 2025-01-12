package vstu.isd.notebin.exception;

import lombok.Getter;

@Getter
public enum ClientExceptionName {
    NOTE_UNAVAILABLE(
            100
    ),
    NOTE_NOT_FOUND(
            101
    ),
    NOT_ALLOWED(
            300
    ),
    VALIDATION_EXCEPTION(
            800
    ),
    GROUP_VALIDATION_EXCEPTION(
            801
    ),
    INVALID_TITLE(
            802
    ),
    INVALID_CONTENT(
            803
    ),
    INVALID_EXPIRATION_TYPE(
           804
    ),
    INVALID_EXPIRATION_PERIOD(
            805
    ),
    EMPTY_UPDATE_REQUEST(
        806
    );

    private final int apiErrorCode;

    ClientExceptionName(int apiErrorCode) {
        this.apiErrorCode = apiErrorCode;
    }
}
