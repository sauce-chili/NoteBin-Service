package vstu.isd.notebin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import vstu.isd.notebin.exception.BaseClientException;
import vstu.isd.notebin.exception.GroupValidationException;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GroupValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseException handleGroupValidationException(GroupValidationException groupExp) {

        ProblemDetail problemDetail = buildBaseClientExceptionProblemDetail(groupExp);
        problemDetail.setProperty("errors", buildGroupValidationExceptionsErrors(groupExp));

        return new ErrorResponseException(groupExp.getStatusCode(), problemDetail, groupExp);
    }

    private ProblemDetail buildBaseClientExceptionProblemDetail(BaseClientException baseClientException) {

        ProblemDetail problemDetail = buildBaseProblemDetail(
                baseClientException.getMessage(),
                baseClientException.getStatusCode() == null ?
                        HttpStatus.INTERNAL_SERVER_ERROR : baseClientException.getStatusCode()
        );

        buildBaseClientExceptionProblemDetailProperties(baseClientException)
                .forEach(problemDetail::setProperty);

        return problemDetail;
    }

    private ProblemDetail buildBaseProblemDetail(String reason, HttpStatusCode httpStatusCode) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                httpStatusCode,
                reason
        );

        problemDetail.setType(URI.create("error"));
        problemDetail.setProperty("date", LocalDateTime.now());

        return problemDetail;
    }

    private List<Map<String, Object>> buildGroupValidationExceptionsErrors(GroupValidationException groupException) {
        return groupException.getExceptions().stream()
                .map(e -> {
                    var simpleExpRepresentation = buildBaseClientExceptionProblemDetailProperties(e);
                    simpleExpRepresentation.put("detail", e.getMessage());
                    return simpleExpRepresentation;
                })
                .toList();
    }

    private Map<String, Object> buildBaseClientExceptionProblemDetailProperties(BaseClientException baseClientException) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("api_error_code", baseClientException.getExceptionName().getApiErrorCode());
        properties.put("api_error_name", baseClientException.getExceptionName().name());
        properties.put("properties", baseClientException.properties());
        return properties;
    }

    @ExceptionHandler(BaseClientException.class)
    public ErrorResponseException handleBaseClientException(BaseClientException baseClientException) {

        ProblemDetail problemDetail = buildBaseClientExceptionProblemDetail(baseClientException);

        return new ErrorResponseException(
                baseClientException.getStatusCode() == null ?
                        HttpStatus.INTERNAL_SERVER_ERROR : baseClientException.getStatusCode(),
                problemDetail,
                baseClientException
        );
    }
}
