package vstu.isd.notebin.cache;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import vstu.isd.notebin.entity.Note;
import vstu.isd.notebin.entity.NoteCacheable;
import vstu.isd.notebin.mapper.NoteMapper;
import vstu.isd.notebin.repository.NoteRepository;

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
            @Qualifier("pageSize") int pageSize
    ) {
        this.noteRepository = noteRepository;
        this.noteMapper = noteMapper;
        PAGE_SIZE = pageSize;
    }

    private final static Pageable EMPTY_PAGEABLE = Pageable.unpaged();

    public List<NoteCacheable> getMostUsedNotes(int amount) {

        List<Note> mostUsedNotes = new LinkedList<>();

        final int INDEX_OF_FIRST_PAGE = 0;
        Pageable pageable = PageRequest.of(INDEX_OF_FIRST_PAGE, PAGE_SIZE);
        int remaining = amount;
        while(remaining > 0 && pageable != EMPTY_PAGEABLE){

            Page<Note> notePage = noteRepository.findAll(pageable);
            mostUsedNotes.addAll(necessaryNotesOnPage(notePage, remaining));

            remaining = amount - mostUsedNotes.size();
            pageable = nextPageable(notePage, pageable);
        }

        return mostUsedNotes.stream()
                .map(noteMapper::toCacheable)
                .collect(Collectors.toList());
    }

    private List<Note> necessaryNotesOnPage(Page<Note> notePage, int necessary) {

        List<Note> currentNotesOnPage = notePage.getContent();
        List<Note> necessaryNotes = currentNotesOnPage.subList(
                0,
                countOfNecessaryNotesOnPage(necessary, currentNotesOnPage)
        );

        return necessaryNotes;
    }

    private int countOfNecessaryNotesOnPage(int remaining, List<Note> currentNotesOnPage) {

        return Math.min(remaining, currentNotesOnPage.size());
    }

    private Pageable nextPageable(Page page, Pageable pageable) {

        if(page.hasNext()){
            return pageable.next();
        }

        return EMPTY_PAGEABLE;
    }
}
