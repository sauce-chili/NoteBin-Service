package vstu.isd.notebin.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Getter
public class NoteNonExistsException extends BaseClientException {
    private final String nonExistNoteUrl;
    public NoteNonExistsException(String url) {
        super(
                String.format("Note with url %s not found", url),
                ClientExceptionName.NOTE_NOT_FOUND,
                HttpStatus.NOT_FOUND
        );
        this.nonExistNoteUrl = url;
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of("url", nonExistNoteUrl);
    }
}
