package vstu.isd.notebin.validation;

import vstu.isd.notebin.exception.ValidationGroupException;

import java.util.Optional;

public class noteValidator {

    public static Optional<ValidationGroupException> validateCreateNoteRequest(){

        // TODO обговорить формат ошибок
        return Optional.empty();
    }
}
