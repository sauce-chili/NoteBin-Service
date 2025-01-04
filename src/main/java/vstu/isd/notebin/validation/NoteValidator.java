package vstu.isd.notebin.validation;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import vstu.isd.notebin.dto.CreateNoteRequestDto;
import vstu.isd.notebin.entity.ExpirationType;
import vstu.isd.notebin.exception.ClientExceptionName;
import vstu.isd.notebin.exception.GroupValidationException;
import vstu.isd.notebin.exception.ValidationException;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class NoteValidator {

    private final String titleRegexp;
    private final String contentRegexp;
    private final int titleLength;

    public NoteValidator(
            @Qualifier("titleRegexp") String titleRegexp,
            @Qualifier("contentRegexp") String contentRegexp,
            @Qualifier("titleLength") int titleLength
    ) {
        this.titleRegexp = titleRegexp;
        this.contentRegexp = contentRegexp;
        this.titleLength = titleLength;
    }

    public Optional<GroupValidationException> validateCreateNoteRequestDto(CreateNoteRequestDto createNoteRequestDto){

        List<ValidationException> exceptions = new LinkedList<>();

        exceptions.addAll(validateTitle(createNoteRequestDto.getTitle()));
        exceptions.addAll(validateContent(createNoteRequestDto.getContent()));
        exceptions.addAll(validateExpirationType(createNoteRequestDto.getExpirationType()));
        exceptions.addAll(validateExpirationPeriod(createNoteRequestDto.getExpirationPeriod()));

        return exceptions.isEmpty() ? Optional.empty() : Optional.of(new GroupValidationException(exceptions));
    }

    protected List<ValidationException> validateTitle(String title){

        List<ValidationException> exceptions = new LinkedList<>();

        if (title == null) {
            String exceptionDescription = "Title is not set";
            exceptions.add(new ValidationException(exceptionDescription, ClientExceptionName.INVALID_TITLE));

            return exceptions;
        }

        if (!Pattern.matches(titleRegexp, title)) {
            String exceptionDescription = "Title must contain only white delimiters. " +
                    "At the same time, the first symbol must be a digit or letter.";

            exceptions.add(new ValidationException(exceptionDescription, ClientExceptionName.INVALID_TITLE));
        }

        if (title.length() > titleLength) {
            String exceptionDescription = "Title length is too long. " +
                    "Max length is " + titleLength + " symbols.";

            exceptions.add(new ValidationException(exceptionDescription, ClientExceptionName.INVALID_TITLE));
        }

        return exceptions;
    }

    protected List<ValidationException> validateContent(String content){

        if (content == null || !Pattern.matches(contentRegexp, content)) {
            String exceptionDescription = (content == null)
                    ? "Content is not set"
                    : "Content must contain at least one digit or letter.";

            return List.of(new ValidationException(exceptionDescription, ClientExceptionName.INVALID_CONTENT));
        }

        return List.of();
    }

    protected List<ValidationException> validateExpirationType(ExpirationType expirationType){

        if (expirationType == null) {
            String exceptionDescription = "Expiration type not set";
            return List.of(new ValidationException(exceptionDescription, ClientExceptionName.INVALID_EXPIRATION_TYPE));
        }

        return List.of();
    }

    protected List<ValidationException> validateExpirationPeriod(Duration expirationPeriod){

        if (expirationPeriod == null) {
            String exceptionDescription = "Expiration period not set";
            return List.of(new ValidationException(exceptionDescription, ClientExceptionName.INVALID_EXPIRATION_PERIOD));
        }

        return List.of();
    }
}