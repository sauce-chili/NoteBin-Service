package vstu.isd.notebin.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
        List<Note> mostUsedNotes = new ArrayList<>();

        int loaded = 0;
        int pageIndex = 0;
        boolean repositoryContainsNotes = true;
        while (loaded < amount && repositoryContainsNotes) {
            Page<Note> notePage = noteRepository.findAll(PageRequest.of(pageIndex, PAGE_SIZE));

            if (notePage.hasContent()){
                mostUsedNotes.addAll(notePage.stream().toList());
                loaded += notePage.getTotalElements();
                pageIndex++;
            } else {
                repositoryContainsNotes = false;
            }
        }

        return mostUsedNotes.stream()
                .map(noteMapper::toCacheable)
                .collect(Collectors.toList());
    }
}
