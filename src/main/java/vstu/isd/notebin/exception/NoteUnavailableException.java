package vstu.isd.notebin.exception;

import lombok.Getter;

import java.util.Map;

@Getter
public class NoteUnavailableException extends BaseClientException {
    private final String unavailableNoteUrl;

    public NoteUnavailableException(String unavailableNoteUrl) {
        super(ClientExpInfo.NOTE_UNAVAILABLE, 404, "TODO", unavailableNoteUrl); // TODO replace on locale tag
        this.unavailableNoteUrl = unavailableNoteUrl;
    }

    @Override
    public Map<String, Object> detailsBody() {
        return Map.of("url", unavailableNoteUrl);
    }
}
