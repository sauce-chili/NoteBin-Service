package vstu.isd.notebin.cache;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import vstu.isd.notebin.config.TestContainersConfig;
import vstu.isd.notebin.entity.ExpirationType;
import vstu.isd.notebin.entity.Note;
import vstu.isd.notebin.entity.NoteCacheable;
import vstu.isd.notebin.mapper.NoteMapper;
import vstu.isd.notebin.repository.NoteRepository;
import vstu.isd.notebin.testutils.ClearableTest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ContextConfiguration(initializers = TestContainersConfig.class)
@Slf4j
public class NoteCacheHeaterTest extends ClearableTest {

    @SpyBean
    private NoteRepository noteRepository;
    @Autowired
    private NoteCacheHeater noteCacheHeater;
    @Autowired
    private NoteMapper noteMapper;
    private final int PAGE_SIZE = 20;

    @Test
    void getMostUsedNotes() {

        Duration expirationPeriod = null;

        Note firstNote = Note.builder()
                .id(1L)
                .title("Title of first note")
                .content("Content of first note")
                .createdAt(LocalDateTime.now())
                .url("1")
                .expirationType(ExpirationType.NEVER)
                .expirationPeriod(expirationPeriod)
                .expirationFrom(null)
                .isAvailable(true)
                .build();
        noteRepository.save(firstNote);

        Note secondNote = Note.builder()
                .id(2L)
                .title("Title of second note")
                .content("Content of second note")
                .createdAt(LocalDateTime.now())
                .url("2")
                .expirationType(ExpirationType.NEVER)
                .expirationPeriod(expirationPeriod)
                .expirationFrom(null)
                .isAvailable(true)
                .build();
        noteRepository.save(secondNote);


        List<NoteCacheable> actual = noteCacheHeater.getMostUsedNotes(2);


        List<NoteCacheable> exp = List.of(
                noteMapper.toCacheable(firstNote),
                noteMapper.toCacheable(secondNote)
        );
        assertEquals(actual, exp);
    }

    @Test
    void fewPagesWhileGetMostUsedNotes() {

        Duration expirationPeriod = null;

        int TOTAL_COUNT_OF_NOTES = PAGE_SIZE * 2;
        List<Note> notesInRep = new LinkedList<>();
        for (int i = 1; i <= TOTAL_COUNT_OF_NOTES; i++) {
            LocalDateTime createdAt = LocalDateTime.now().minusMinutes(i);
            Note note = Note.builder()
                    .id((long) i)
                    .title("Title of note " + i)
                    .content("Content of note " + i)
                    .createdAt(createdAt)
                    .url(String.valueOf(i))
                    .expirationType(ExpirationType.NEVER)
                    .expirationPeriod(expirationPeriod)
                    .expirationFrom(null)
                    .isAvailable(true)
                    .build();
            notesInRep.add(note);
            noteRepository.save(note);
        }

        List<NoteCacheable> actual = noteCacheHeater.getMostUsedNotes(TOTAL_COUNT_OF_NOTES);

        List<NoteCacheable> exp = notesInRep.stream()
                .map(noteMapper::toCacheable)
                .collect(Collectors.toList());
        assertEquals(exp, actual);
    }

    @Test
    void heaterReturnLessThanRequired() {

        Duration expirationPeriod = null;

        int TOTAL_COUNT_OF_NOTES = PAGE_SIZE * 2;
        List<Note> notesInRep = new LinkedList<>();
        for (int i = 1; i <= TOTAL_COUNT_OF_NOTES; i++) {
            LocalDateTime createdAt = LocalDateTime.now().minusMinutes(i);
            Note note = Note.builder()
                    .id((long) i)
                    .title("Title of note " + i)
                    .content("Content of note " + i)
                    .createdAt(createdAt)
                    .url(String.valueOf(i))
                    .expirationType(ExpirationType.NEVER)
                    .expirationPeriod(expirationPeriod)
                    .expirationFrom(null)
                    .isAvailable(true)
                    .build();
            notesInRep.add(note);
            noteRepository.save(note);
        }

        List<NoteCacheable> actual = noteCacheHeater.getMostUsedNotes(TOTAL_COUNT_OF_NOTES * 2);

        List<NoteCacheable> exp = notesInRep.stream()
                .map(noteMapper::toCacheable)
                .collect(Collectors.toList());
        assertEquals(exp, actual);
    }
}