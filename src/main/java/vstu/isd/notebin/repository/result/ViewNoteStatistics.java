package vstu.isd.notebin.repository.result;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ViewNoteStatistics {
    private final Long anonymousViews;
    private final Long userViews;
}
