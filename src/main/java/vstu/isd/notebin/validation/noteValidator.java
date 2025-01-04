package vstu.isd.notebin.validation;

import org.springframework.beans.factory.annotation.Qualifier;
import vstu.isd.notebin.dto.CreateNoteRequestDto;
import vstu.isd.notebin.exception.BaseClientException;
import vstu.isd.notebin.exception.ClientExpInfo;
import vstu.isd.notebin.exception.ValidationGroupException;

import java.util.Optional;

public class noteValidator {

    private final String titleRegexp;
    private final String contentRegexp;
    private final int titleLength;

    public noteValidator(
            @Qualifier("titleRegexp") String titleRegexp,
            @Qualifier("contentRegexp") String contentRegexp,
            @Qualifier("titleLength") int titleLength
    ) {
        this.titleRegexp = titleRegexp;
        this.contentRegexp = contentRegexp;
        this.titleLength = titleLength;
    }

    public static Optional<ValidationGroupException> validateCreateNoteRequestDto(CreateNoteRequestDto createNoteRequestDto){

        List<BaseClientException> exceptions = new ArrayList<>();

        // TODO обговорить формат ошибок
        return Optional.empty();
    }
}
