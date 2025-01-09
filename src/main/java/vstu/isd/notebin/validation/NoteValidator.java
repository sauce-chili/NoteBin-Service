package vstu.isd.notebin.validation;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import vstu.isd.notebin.dto.CreateNoteRequestDto;
import vstu.isd.notebin.dto.UpdateNoteRequestDto;
import vstu.isd.notebin.entity.BaseNote;
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
        exceptions.addAll(validateExpirationPeriod(createNoteRequestDto.getExpirationPeriod(),
                createNoteRequestDto.getExpirationType()));

        return exceptions.isEmpty() ? Optional.empty() : Optional.of(new GroupValidationException(exceptions));
    }

    public List<ValidationException> validateTitle(String title){

        List<ValidationException> exceptions = new LinkedList<>();

        if (title == null) {
            String exceptionDescription = "Title is not set";
            exceptions.add(new ValidationException(exceptionDescription, ClientExceptionName.INVALID_TITLE));
        }

        if(exceptions.isEmpty()){
            exceptions.addAll(validateTitleByContent(title));
        }

        return exceptions;
    }

    private List<ValidationException> validateTitleByContent(String title){

        List<ValidationException> exceptions = new LinkedList<>();

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

    public List<ValidationException> validateContent(String content){

        List<ValidationException> exceptions = new LinkedList<>();

        exceptions.addAll(validateContentForNotNull(content));

        if(exceptions.isEmpty()){
            exceptions.addAll(validateContentByContent(content));
        }

        return exceptions;
    }

    private List<ValidationException> validateContentForNotNull(String content){

        if (content == null) {
            String exceptionDescription = "Content is not set";
            return List.of(new ValidationException(exceptionDescription, ClientExceptionName.INVALID_CONTENT));
        }

        return List.of();
    }

    private List<ValidationException> validateContentByContent(String content){

        if (!Pattern.matches(contentRegexp, content)) {
            String exceptionDescription = "Content must contain at least one digit or letter.";
            return List.of(new ValidationException(exceptionDescription, ClientExceptionName.INVALID_CONTENT));
        }

        return List.of();
    }

    public List<ValidationException> validateExpirationType(ExpirationType expirationType){

        if (expirationType == null) {
            String exceptionDescription = "Expiration type not set";
            return List.of(new ValidationException(exceptionDescription, ClientExceptionName.INVALID_EXPIRATION_TYPE));
        }

        return List.of();
    }

    public List<ValidationException> validateExpirationPeriod(Duration expirationPeriod, ExpirationType expirationType){

        if (expirationType == ExpirationType.NEVER && expirationPeriod != null) {
            String exceptionDescription = "Expiration period must be not set when expiration type is NEVER";
            return List.of(new ValidationException(exceptionDescription, ClientExceptionName.INVALID_EXPIRATION_PERIOD));
        }

        if (expirationType == ExpirationType.BURN_AFTER_READ && expirationPeriod != null) {
            String exceptionDescription = "Expiration period must be not set when expiration type is BURN AFTER READ";
            return List.of(new ValidationException(exceptionDescription, ClientExceptionName.INVALID_EXPIRATION_PERIOD));
        }

        if (expirationType == ExpirationType.BURN_BY_PERIOD && expirationPeriod == null) {
            String exceptionDescription = "Expiration period must be set when expiration type is BURN BY PERIOD";
            return List.of(new ValidationException(exceptionDescription, ClientExceptionName.INVALID_EXPIRATION_PERIOD));
        }

        return List.of();
    }

    public <T extends BaseNote> Optional<ValidationException> validatePersistedAndUpdateRequest(T persisted, UpdateNoteRequestDto updateRequest) {

        if (updateRequest.getExpirationType() != null){

            return Optional.empty();
        }

        if (updateRequest.getExpirationPeriod() == null){
            if (persisted.getExpirationType() == ExpirationType.BURN_BY_PERIOD) {
                String description = "Note has expiration type " + ExpirationType.BURN_BY_PERIOD + ". Expiration type doesn't update. " +
                        "That's mean expirationPeriod must be set.";
                return Optional.of(
                        new ValidationException( description, ClientExceptionName.INVALID_EXPIRATION_PERIOD)
                );
            }
        } else {
            if (persisted.getExpirationType() != ExpirationType.BURN_BY_PERIOD) {
                String description = "expirationPeriod updates, expirationType doesn't update. " +
                        "But expirationType of note is " + persisted.getExpirationType() +
                        ". Hence, expirationPeriod must me not set.";
                return Optional.of(
                        new ValidationException( description, ClientExceptionName.INVALID_EXPIRATION_PERIOD)
                );
            }
        }

        return Optional.empty();
    }

    public Optional<GroupValidationException> validateUpdateNoteRequestDto(UpdateNoteRequestDto updateNoteRequestDto){

        List<ValidationException> exceptions = new LinkedList<>();

        exceptions.addAll(validateUpdateNoteRequestDtoForAllFieldsNotNull(updateNoteRequestDto));
        boolean allFieldsAreNull = !exceptions.isEmpty();
        if (allFieldsAreNull){
            return Optional.of(new GroupValidationException(exceptions));
        }

        if (updateNoteRequestDto.getTitle() != null){
            exceptions.addAll(validateTitleByContent(updateNoteRequestDto.getTitle()));
        }

        if (updateNoteRequestDto.getContent() != null){
            exceptions.addAll(validateContentByContent(updateNoteRequestDto.getContent()));
        }

        if (updateNoteRequestDto.getExpirationType() != null){
            exceptions.addAll(validateExpirationPeriod(updateNoteRequestDto.getExpirationPeriod(),
                    updateNoteRequestDto.getExpirationType()));
        }

        return exceptions.isEmpty() ? Optional.empty() : Optional.of(new GroupValidationException(exceptions));
    }

    private List<ValidationException> validateUpdateNoteRequestDtoForAllFieldsNotNull(UpdateNoteRequestDto updateNoteRequestDto){

        if     (updateNoteRequestDto.getTitle()            == null &&
                updateNoteRequestDto.getContent()          == null &&
                updateNoteRequestDto.getExpirationType()   == null &&
                updateNoteRequestDto.getExpirationPeriod() == null &&
                updateNoteRequestDto.getIsAvailable()      == null) {

            String exceptionDescription = "All fields in update request are not set.";
            return List.of(new ValidationException(exceptionDescription, ClientExceptionName.INVALID_UPDATE_NOTE_REQUEST_DTO));
        }

        return List.of();
    }
}