package vstu.isd.notebin.exception;

import lombok.Getter;

@Getter
public enum ClientExpInfo {
    NOTE_UNAVAILABLE(
            100,
            "NOTE_UNAVAILABLE"
    ),
    NOTE_NOT_FOUND(
            101,
            "NOTE_NOT_FOUND"
    ),
    INVALID_TITLE(
            800,
            "INVALID_TITLE"
    ),
    INVALID_CONTENT(
            801,
            "INVALID_CONTENT"
    );

    private final int apiErrorCode;
    private final String errorName;

    ClientExpInfo(int errorCode, String errorMsg) {
        this.apiErrorCode = errorCode;
        this.errorName = errorMsg;
    }
}
