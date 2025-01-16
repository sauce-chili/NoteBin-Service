package vstu.isd.notebin.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
@Slf4j
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
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("api_error_code", baseClientException.getExceptionName().getApiErrorCode());
        props.put("api_error_name", baseClientException.getExceptionName().name());
        props.put("args", baseClientException.properties());
        return props;
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

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponseException handleException(Exception e) {
        log.error("Unexpected exception: ", e);
        return new ErrorResponseException(HttpStatus.INTERNAL_SERVER_ERROR, e);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponseException handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request body format");
        problemDetail.setType(URI.create("error"));
        problemDetail.setTitle("Invalid request");
        return new ErrorResponseException(HttpStatus.BAD_REQUEST, problemDetail, ex);
    }
}
