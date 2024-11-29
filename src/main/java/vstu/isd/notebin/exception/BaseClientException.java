package vstu.isd.notebin.exception;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public abstract class BaseClientException extends RuntimeException {
    private final String errorName;
    private final int apiErrorCode;
    private final int statusCode;
    protected String detailsMessageLocaleTag;
    protected Object[] detailsMessageArgs;

    public BaseClientException(
            ClientExpInfo clientExpInfo,
            int statusCode,
            String detailsMessageLocaleTag,
            Object... detailsMessageArgs
    ) {
        this(clientExpInfo, statusCode);
        this.detailsMessageLocaleTag = detailsMessageLocaleTag;
        this.detailsMessageArgs = detailsMessageArgs;
    }

    public BaseClientException(ClientExpInfo clientExpInfo, int statusCode) {
        this(clientExpInfo.getErrorName(), clientExpInfo.getApiErrorCode(), statusCode);
    }

    public BaseClientException(String errorName, int errorCode, int statusCode) {
        this.errorName = errorName;
        this.apiErrorCode = errorCode;
        this.statusCode = statusCode;
    }

    public BaseClientException(
            String errorName,
            int errorCode,
            int statusCode,
            String detailsMessageLocaleTag,
            Object... detailsMessageArgs
    ) {
        this.errorName = errorName;
        this.apiErrorCode = errorCode;
        this.statusCode = statusCode;
        this.detailsMessageLocaleTag = detailsMessageLocaleTag;
        this.detailsMessageArgs = detailsMessageArgs;
    }

    public Map<String, Object> detailsBody() {
        return new HashMap<>();
    }
}
