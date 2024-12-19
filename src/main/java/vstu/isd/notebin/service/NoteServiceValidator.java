package vstu.isd.notebin.service;

import vstu.isd.notebin.exception.ValidationGroupException;

import java.util.Optional;

public class NoteServiceValidator {

    public static Optional<ValidationGroupException> validateCreateNoteRequest(){

        // TODO обговорить формат ошибок
        return Optional.empty();
    }
}
