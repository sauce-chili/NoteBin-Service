package vstu.isd.notebin.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import vstu.isd.notebin.entity.Note;
import vstu.isd.notebin.entity.NoteCacheable;
import vstu.isd.notebin.mapper.NoteMapper;
import vstu.isd.notebin.repository.NoteRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NoteCacheHeater {
    private final NoteRepository noteRepository;
    private final NoteMapper noteMapper;

    public List<NoteCacheable> getMostUsedNotes(int amount) {
        List<Note> mostUsedNotes = new ArrayList<>();
        int PAGE_SIZE = 20;

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
