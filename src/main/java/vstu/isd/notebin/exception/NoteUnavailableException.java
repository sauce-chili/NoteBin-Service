package vstu.isd.notebin.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Getter
public class NoteUnavailableException extends BaseClientException {
    private final String unavailableNoteUrl;

    public NoteUnavailableException(String unavailableNoteUrl) {
        super(
                String.format("Note with url %s is unavailable", unavailableNoteUrl),
                ClientExceptionName.NOTE_UNAVAILABLE,
                HttpStatus.NOT_FOUND
        );
        this.unavailableNoteUrl = unavailableNoteUrl;
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of("url", unavailableNoteUrl);
    }
}
