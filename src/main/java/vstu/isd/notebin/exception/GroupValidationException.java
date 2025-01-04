package vstu.isd.notebin.exception;


import lombok.Getter;

import java.util.List;

public class GroupValidationException extends ValidationException {

    @Getter
    private final List<? extends ValidationException> exceptions;

    public GroupValidationException(List<ValidationException> exceptions) {
        super(
                "Group validation exception.",
                ClientExceptionName.GROUP_VALIDATION_EXCEPTION
        );
        this.exceptions = exceptions;
    }
}
