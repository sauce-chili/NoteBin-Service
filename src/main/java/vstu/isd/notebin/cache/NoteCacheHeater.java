package vstu.isd.notebin.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import vstu.isd.notebin.entity.NoteCacheable;
import vstu.isd.notebin.repository.NoteRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class NoteCacheHeater {
    private final NoteRepository noteRepository;

    public List<NoteCacheable> getMostUsedNotes(int amount) {
        return List.of(); // TODO implement
    }
}
