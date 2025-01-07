package vstu.isd.notebin.validation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import vstu.isd.notebin.dto.CreateNoteRequestDto;
import vstu.isd.notebin.entity.ExpirationType;
import vstu.isd.notebin.exception.ClientExceptionName;
import vstu.isd.notebin.exception.GroupValidationException;
import vstu.isd.notebin.exception.ValidationException;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Slf4j
class NoteValidatorForCreateNoteDtoTest {

    @Autowired
    private NoteValidator noteValidator;

    // title -----------------------------------------------------------------------------------------------------------
    @Test
    void titleIsNull() {
        String title = null;

        List<ValidationException> actual = noteValidator.validateTitle(title);

        assertEquals(1, actual.size());
        assertEquals(ClientExceptionName.INVALID_TITLE, actual.get(0).getExceptionName());
    }

    @Test
    void titleStartsWithWhiteDelimiter() {
        String title = ",Title";

        List<ValidationException> actual = noteValidator.validateTitle(title);

        assertEquals(1, actual.size());
        assertEquals(ClientExceptionName.INVALID_TITLE, actual.get(0).getExceptionName());
    }

    @Test
    void titleContainsUndefinedSymbol() {
        String title = "Tit+le";

        List<ValidationException> actual = noteValidator.validateTitle(title);

        assertEquals(1, actual.size());
        assertEquals(ClientExceptionName.INVALID_TITLE, actual.get(0).getExceptionName());
    }

    @Test
    void titleStartsWithDigit() {
        String title = "2Title";

        List<ValidationException> actual = noteValidator.validateTitle(title);

        assertEquals(0, actual.size());
    }

    @Test
    void titleStartsWithCapitalLetter() {
        String title = "Title";

        List<ValidationException> actual = noteValidator.validateTitle(title);

        assertEquals(0, actual.size());
    }

    @Test
    void titleStartsWithLowercaseLetter() {
        String title = "title";

        List<ValidationException> actual = noteValidator.validateTitle(title);

        assertEquals(0, actual.size());
    }

    @Test
    void titleContainsWhiteDelimiterNotInTheStart() {
        String title = "tit,le";

        List<ValidationException> actual = noteValidator.validateTitle(title);

        assertEquals(0, actual.size());
    }

    @Test
    void titleLengthIsHigherThanMax() {
        String title = "snBkSFkghmcmCBvWDksdGfnIJdxvkqEergXjqfbsDhiAgUjMKVjXOXSgpaqkkWLlMFREzvkPgRXvVnDKvixysCCUGMhHzwqBnxqZkkDMKDnhaltnKyXgLuQagrZxNSFbhM";

        List<ValidationException> actual = noteValidator.validateTitle(title);

        assertEquals(1, actual.size());
        assertEquals(ClientExceptionName.INVALID_TITLE, actual.get(0).getExceptionName());
    }

    @Test
    void titleLengthIsMax() {
        String title = "snBkSFkghmcmCBvWDksdGfnIJdxvkqEergXjqfbsDhiAgUjMKVjXOXSgpaqkkWLlMFREzvkPgRXvVnDKvixysCCUGMhHzwqBnxqZkkDMKDnhaltnKyXgLuQagrZxNSFb";

        List<ValidationException> actual = noteValidator.validateTitle(title);

        assertEquals(0, actual.size());
    }

    @Test
    void titleContainsAllAllowedCharacters() {
        String title = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 .,!?;:()[]{}\"'";

        List<ValidationException> actual = noteValidator.validateTitle(title);

        assertEquals(0, actual.size());
    }

    // content ---------------------------------------------------------------------------------------------------------
    @Test
    void contentIsNull() {
        String content = null;

        List<ValidationException> actual = noteValidator.validateContent(content);

        assertEquals(1, actual.size());
        assertEquals(ClientExceptionName.INVALID_CONTENT, actual.get(0).getExceptionName());
    }

    @Test
    void contentStartsWithWhiteDelimiter() {
        String content = ",Content";

        List<ValidationException> actual = noteValidator.validateContent(content);

        assertEquals(0, actual.size());
    }

    @Test
    void contentContainsUndefinedSymbol() {
        String content = "Con+tent";

        List<ValidationException> actual = noteValidator.validateContent(content);

        assertEquals(0, actual.size());
    }

    @Test
    void contentStartsWithDigit() {
        String content = "2Content";

        List<ValidationException> actual = noteValidator.validateContent(content);

        assertEquals(0, actual.size());
    }

    @Test
    void contentStartsWithCapitalLetter() {
        String content = "Content";

        List<ValidationException> actual = noteValidator.validateContent(content);

        assertEquals(0, actual.size());
    }

    @Test
    void contentStartsWithLowercaseLetter() {
        String content = "content";

        List<ValidationException> actual = noteValidator.validateContent(content);

        assertEquals(0, actual.size());
    }

    @Test
    void contentContainsWhiteDelimiterNotInTheStart() {
        String content = "con,tent";

        List<ValidationException> actual = noteValidator.validateContent(content);

        assertEquals(0, actual.size());
    }

    @Test
    void contentDoesNotContainOnlyWhiteDelimiters() {
        String content = ",.,.,.,., .,. .,. ,,. ,. ,";

        List<ValidationException> actual = noteValidator.validateContent(content);

        assertEquals(1, actual.size());
        assertEquals(ClientExceptionName.INVALID_CONTENT, actual.get(0).getExceptionName());
    }

    @Test
    void contentDoesNotContainDigitsOrLetters() {
        String content = ",.,.+= //.,., +--_";

        List<ValidationException> actual = noteValidator.validateContent(content);

        assertEquals(1, actual.size());
        assertEquals(ClientExceptionName.INVALID_CONTENT, actual.get(0).getExceptionName());
    }

    // expiration type -------------------------------------------------------------------------------------------------
    @Test
    void expirationTypeIsNull() {
        ExpirationType expirationType = null;

        List<ValidationException> actual = noteValidator.validateExpirationType(expirationType);

        assertEquals(1, actual.size());
        assertEquals(ClientExceptionName.INVALID_EXPIRATION_TYPE, actual.get(0).getExceptionName());
    }

    // expiration period -----------------------------------------------------------------------------------------------

    @Test
    void expirationPeriodNotSetWhileExpirationTypeIsBurnAfterRead(){

        ExpirationType expirationType = ExpirationType.BURN_AFTER_READ;
        Duration expirationPeriod = null;

        List<ValidationException> actual = noteValidator.validateExpirationPeriod(expirationPeriod, expirationType);

        assertEquals(0, actual.size());
    }

    @Test
    void expirationPeriodNotSetWhileExpirationTypeIsNever(){

        ExpirationType expirationType = ExpirationType.NEVER;
        Duration expirationPeriod = null;

        List<ValidationException> actual = noteValidator.validateExpirationPeriod(expirationPeriod, expirationType);

        assertEquals(0, actual.size());
    }

    @Test
    void expirationPeriodSetWhileExpirationTypeIsBurnByPeriod(){

        ExpirationType expirationType = ExpirationType.BURN_BY_PERIOD;
        Duration expirationPeriod = Duration.ofMinutes(15);

        List<ValidationException> actual = noteValidator.validateExpirationPeriod(expirationPeriod, expirationType);

        assertEquals(0, actual.size());
    }

    @Test
    void expirationPeriodNotSetWhenExpirationTypeIsBurnByPeriod(){

        ExpirationType expirationType = ExpirationType.BURN_BY_PERIOD;
        Duration expirationPeriod = null;

        List<ValidationException> actual = noteValidator.validateExpirationPeriod(expirationPeriod, expirationType);

        assertEquals(1, actual.size());
        assertEquals(ClientExceptionName.INVALID_EXPIRATION_PERIOD, actual.get(0).getExceptionName());
    }

    @Test
    void expirationPeriodSetWhenExpirationTypeIsNever(){

        ExpirationType expirationType = ExpirationType.NEVER;
        Duration expirationPeriod = Duration.ofMinutes(15);

        List<ValidationException> actual = noteValidator.validateExpirationPeriod(expirationPeriod, expirationType);

        assertEquals(1, actual.size());
        assertEquals(ClientExceptionName.INVALID_EXPIRATION_PERIOD, actual.get(0).getExceptionName());
    }

    @Test
    void expirationPeriodSetWhenExpirationTypeIsBurnAfterRead(){

        ExpirationType expirationType = ExpirationType.BURN_AFTER_READ;
        Duration expirationPeriod = Duration.ofMinutes(15);

        List<ValidationException> actual = noteValidator.validateExpirationPeriod(expirationPeriod, expirationType);

        assertEquals(1, actual.size());
        assertEquals(ClientExceptionName.INVALID_EXPIRATION_PERIOD, actual.get(0).getExceptionName());
    }

    // GroupValidationException ----------------------------------------------------------------------------------------
    @Test
    void exceptionsForEveryField(){

        Duration expirationPeriod = null;
        String title = ",snBkSFkghmcmCBvWDksdGfnIJdxvkqEergXjqfbsDhiAgUjMKVjXOXSgpaqkkWLlMFREzvkPgRXvVnDKvixysCCUGMhHzwqBnxqZkkDMKDnhaltnKyXgLuQagrZxNSFbhM";
        String content = "  ., ";
        CreateNoteRequestDto createNoteRequestDto = CreateNoteRequestDto.builder()
                .title(title)
                .content(content)
                .expirationType(null)
                .expirationPeriod(expirationPeriod)
                .build();

        Optional<GroupValidationException> optionalGroupOfExceptions =
                noteValidator.validateCreateNoteRequestDto(createNoteRequestDto);

        assertTrue(optionalGroupOfExceptions.isPresent());
        GroupValidationException groupValidationException = optionalGroupOfExceptions.get();
        List<? extends ValidationException> exceptions = groupValidationException.getExceptions();

        assertEquals(4, exceptions.size());
        assertEquals(ClientExceptionName.INVALID_TITLE, exceptions.get(0).getExceptionName());
        assertEquals(ClientExceptionName.INVALID_TITLE, exceptions.get(1).getExceptionName());
        assertEquals(ClientExceptionName.INVALID_CONTENT, exceptions.get(2).getExceptionName());
        assertEquals(ClientExceptionName.INVALID_EXPIRATION_TYPE, exceptions.get(3).getExceptionName());
    }
}