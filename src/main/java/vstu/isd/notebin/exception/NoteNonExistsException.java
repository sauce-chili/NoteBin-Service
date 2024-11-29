package vstu.isd.notebin.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class NoteNonExistsException extends BaseClientException {
    private final String nonExistNoteUrl;

    public NoteNonExistsException(String url) {
        super(ClientExpInfo.NOTE_NOT_FOUND, 404, "TODO", url); // TODO replace on locale tag
        this.nonExistNoteUrl = url;
    }

    @Override
    public Map<String, Object> detailsBody() {
        return Map.of("url", nonExistNoteUrl);
    }
}
