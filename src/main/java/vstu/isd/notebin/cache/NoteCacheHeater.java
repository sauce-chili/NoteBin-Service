package vstu.isd.notebin.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import vstu.isd.notebin.config.NoteConfig;
import vstu.isd.notebin.entity.Note;
import vstu.isd.notebin.entity.NoteCacheable;
import vstu.isd.notebin.mapper.NoteMapper;
import vstu.isd.notebin.repository.NoteRepository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class NoteCacheHeater {

    private final NoteRepository noteRepository;
    private final NoteMapper noteMapper;
    private final int PAGE_SIZE;

    NoteCacheHeater(
            NoteRepository noteRepository,
            NoteMapper noteMapper,
            int pageSize
    ) {
        this.noteRepository = noteRepository;
        this.noteMapper = noteMapper;
        PAGE_SIZE = pageSize;
    }

    public List<NoteCacheable> getMostUsedNotes(int amount) {
        List<Note> mostUsedNotes = new LinkedList<>();

        final int INDEX_OF_FIRST_PAGE = 0;
        int totalPages = noteRepository.findAll(PageRequest.of(INDEX_OF_FIRST_PAGE, PAGE_SIZE)).getTotalPages();

        int loaded = 0;
        int currentPageIndex = 0;
        while (nextPageExists(currentPageIndex, totalPages) && notEnoughLoaded(loaded, amount)) {

            Page<Note> notePage = noteRepository.findAll(PageRequest.of(currentPageIndex, PAGE_SIZE));

            mostUsedNotes.addAll(notePage.stream().toList());
            loaded += notePage.getTotalElements();
            currentPageIndex++;
        }

        return mostUsedNotes.stream()
                .map(noteMapper::toCacheable)
                .collect(Collectors.toList());
    }

    private boolean nextPageExists(int currentPageIndex, int totalPages){
        return currentPageIndex + 1 <= totalPages;
    }

    private boolean notEnoughLoaded(int loaded, int required) {
        return loaded < required;
    }
}
