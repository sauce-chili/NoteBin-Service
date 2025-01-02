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
    );

    private final int apiErrorCode;

    ClientExceptionName(int apiErrorCode) {
        this.apiErrorCode = apiErrorCode;
    }
}
