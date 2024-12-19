package vstu.isd.notebin.exception;

public class ValidationGroupException extends BaseClientException{

    public ValidationGroupException(ClientExpInfo clientExpInfo, int statusCode, String detailsMessageLocaleTag, Object... detailsMessageArgs) {
        super(clientExpInfo, statusCode, detailsMessageLocaleTag, detailsMessageArgs);
    }


}
