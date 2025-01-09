package vstu.isd.notebin.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import vstu.isd.notebin.cache.NoteCache;
import vstu.isd.notebin.config.TestContainersConfig;
import vstu.isd.notebin.dto.GetNoteRequestDto;
import vstu.isd.notebin.dto.NoteDto;
import vstu.isd.notebin.entity.ExpirationType;
import vstu.isd.notebin.entity.Note;
import vstu.isd.notebin.entity.NoteCacheable;
import vstu.isd.notebin.exception.*;
import vstu.isd.notebin.repository.NoteRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static vstu.isd.notebin.testutils.TestAsserts.*;

@SpringBootTest
@ContextConfiguration(initializers = TestContainersConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Slf4j
public class NoteServiceGetNoteTest {

    @SpyBean
    private NoteRepository noteRepository;
    @SpyBean
    private NoteCache noteCache;
    @Autowired
    private NoteService noteService;

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;

    private final static ExecutorService executors = Executors.newFixedThreadPool(MAXIMUM_POOL_SIZE);

    /*
     * Tests for NoteService.getNote(...)
     *
     * Aspects of testing:
     * - getting note with different expiration statuses: not expired, already expired, expired at the moment of getting (+)
     * - location of note storage at time of request: cache and repository, repository, nowhere(exception) (+-)
     * - note expiration during concurrent getting a note: BURN_AFTER_READ, BURN_BY_PERIOD expiration types (+)
     * */
    @Test
    public void getNoteWithBurnAfterReadExpiration() {

        final String REQUESTED_NOTE_URL = "0";

        LocalDateTime now = LocalDateTime.now();
        Note persistedRepoNote = Note.builder()
                .url(REQUESTED_NOTE_URL)
                .isAvailable(true)
                .title("title")
                .content("content")
                .expirationType(ExpirationType.BURN_AFTER_READ)
                .createdAt(now)
                .expirationFrom(null)
                .build();
        persistedRepoNote = noteRepository.save(persistedRepoNote);

        NoteCacheable persistedCacheNote = NoteCacheable.builder()
                .url(REQUESTED_NOTE_URL)
                .id(persistedRepoNote.getId())
                .isAvailable(true)
                .title("title")
                .content("content")
                .expirationType(ExpirationType.BURN_AFTER_READ)
                .createdAt(now)
                .expirationFrom(null)
                .build();
        noteCache.save(persistedCacheNote);


        NoteDto actualNote = noteService.getNote(new GetNoteRequestDto(REQUESTED_NOTE_URL));
        NoteDto expectedNote = NoteDto.builder()
                .url(REQUESTED_NOTE_URL)
                .id(persistedRepoNote.getId())
                .isAvailable(false)
                .title("title")
                .content("content")
                .expirationType(ExpirationType.BURN_AFTER_READ)
                .createdAt(now)
                .expirationFrom(null)
                .build();

        assertNoteDtoEquals(expectedNote, actualNote);


        Note actualRepoNote = noteRepository.findByUrl(REQUESTED_NOTE_URL).get();
        Note expectedRepoNote = persistedRepoNote.toBuilder()
                .isAvailable(false)
                .build();

        assertEquals(expectedRepoNote, actualRepoNote);


        NoteCacheable actualCacheNote = noteCache.get(REQUESTED_NOTE_URL).get();
        NoteCacheable expectedCacheNote = persistedCacheNote.toBuilder()
                .isAvailable(false)
                .build();

        assertEquals(expectedCacheNote, actualCacheNote);
    }

    @Test
    public void concurrentGetNoteWithBurnAfterReadExpiration() {
        final String REQUESTED_NOTE_URL = "1";

        LocalDateTime now = LocalDateTime.now();
        Note persistedRepoNote = Note.builder()
                .url(REQUESTED_NOTE_URL)
                .isAvailable(true)
                .title("title")
                .content("content")
                .expirationType(ExpirationType.BURN_AFTER_READ)
                .createdAt(now)
                .expirationFrom(null)
                .build();
        persistedRepoNote = noteRepository.save(persistedRepoNote);

        NoteCacheable persistedCacheNote = NoteCacheable.builder()
                .url(REQUESTED_NOTE_URL)
                .id(persistedRepoNote.getId())
                .isAvailable(true)
                .title("title")
                .content("content")
                .expirationType(ExpirationType.BURN_AFTER_READ)
                .createdAt(now)
                .expirationFrom(null)
                .build();
        noteCache.save(persistedCacheNote);

        Supplier<Optional<NoteDto>> requestAction = () -> {
            try {
                return Optional.of(noteService.getNote(new GetNoteRequestDto(REQUESTED_NOTE_URL)));
            } catch (NoteUnavailableException e) {
                return Optional.empty();
            }
        };

        List<CompletableFuture<Optional<NoteDto>>> futures = Stream.generate(
                        () -> CompletableFuture.supplyAsync(requestAction, executors)
                )
                .limit(CPU_COUNT)
                .toList();

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        List<Optional<NoteDto>> requestResults = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        long availableNotes = requestResults.stream().filter(Optional::isPresent).count();
        long unavailableNotes = requestResults.stream().filter(Optional::isEmpty).count();

        assertEquals(1, availableNotes);
        assertEquals(CPU_COUNT - 1, unavailableNotes);

        NoteDto actualNote = requestResults.stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElseThrow();
        NoteDto expectedNote = NoteDto.builder()
                .id(persistedRepoNote.getId())
                .url(REQUESTED_NOTE_URL)
                .isAvailable(false)
                .title("title")
                .content("content")
                .expirationType(ExpirationType.BURN_AFTER_READ)
                .createdAt(now)
                .expirationFrom(null)
                .build();

        assertNoteDtoEquals(expectedNote, actualNote);

        Note actualRepoNote = noteRepository.findByUrl(REQUESTED_NOTE_URL).get();
        Note expectedRepoNote = persistedRepoNote.toBuilder()
                .isAvailable(false)
                .build();

        assertEquals(expectedRepoNote, actualRepoNote);

        NoteCacheable actualCacheNote = noteCache.get(REQUESTED_NOTE_URL).get();
        NoteCacheable expectedCacheNote = persistedCacheNote.toBuilder()
                .isAvailable(false)
                .build();

        assertEquals(expectedCacheNote, actualCacheNote);
    }

    @Test
    public void getNoteWithBurnByPeriodExpiration() {
        final String REQUESTED_NOTE_URL = "2";

        LocalDateTime createAt = LocalDateTime.now().minusDays(1);
        Duration expirationPeriod = Duration.ofMinutes(15);

        Note persistedRepoNote = Note.builder()
                .url(REQUESTED_NOTE_URL)
                .isAvailable(true)
                .title("title")
                .content("content")
                .expirationType(ExpirationType.BURN_BY_PERIOD)
                .createdAt(createAt)
                .expirationFrom(createAt)
                .expirationPeriod(expirationPeriod)
                .build();
        persistedRepoNote = noteRepository.save(persistedRepoNote);

        NoteCacheable persistedCacheNote = NoteCacheable.builder()
                .url(REQUESTED_NOTE_URL)
                .id(persistedRepoNote.getId())
                .isAvailable(persistedRepoNote.isAvailable())
                .title(persistedRepoNote.getTitle())
                .content(persistedRepoNote.getContent())
                .expirationType(persistedRepoNote.getExpirationType())
                .createdAt(persistedRepoNote.getCreatedAt())
                .expirationFrom(persistedRepoNote.getExpirationFrom())
                .expirationPeriod(persistedRepoNote.getExpirationPeriod())
                .build();
        noteCache.save(persistedCacheNote);


        assertThrows(NoteUnavailableException.class, () -> noteService.getNote(
                new GetNoteRequestDto(REQUESTED_NOTE_URL)
        ));


        Note actualRepoNote = noteRepository.findByUrl(REQUESTED_NOTE_URL).get();
        Note expectedRepoNote = persistedRepoNote.toBuilder()
                .isAvailable(false)
                .build();

        assertEquals(expectedRepoNote, actualRepoNote);

        NoteCacheable actualCacheNote = noteCache.get(REQUESTED_NOTE_URL).get();
        NoteCacheable expectedCacheNote = persistedCacheNote.toBuilder()
                .isAvailable(false)
                .build();

        assertEquals(expectedCacheNote, actualCacheNote);
    }

    @Test
    public void concurrentGetNoteWithBurnByPeriodExpiration() {
        final String REQUESTED_NOTE_URL = "3";

        LocalDateTime createAt = LocalDateTime.now().minusDays(1);
        Duration expirationPeriod = Duration.ofMinutes(15);

        Note persistedRepoNote = Note.builder()
                .url(REQUESTED_NOTE_URL)
                .isAvailable(true)
                .title("title")
                .content("content")
                .expirationType(ExpirationType.BURN_BY_PERIOD)
                .createdAt(createAt)
                .expirationFrom(createAt)
                .expirationPeriod(expirationPeriod)
                .build();
        persistedRepoNote = noteRepository.save(persistedRepoNote);

        NoteCacheable persistedCacheNote = NoteCacheable.builder()
                .url(REQUESTED_NOTE_URL)
                .id(persistedRepoNote.getId())
                .isAvailable(persistedRepoNote.isAvailable())
                .title(persistedRepoNote.getTitle())
                .content(persistedRepoNote.getContent())
                .expirationType(persistedRepoNote.getExpirationType())
                .createdAt(persistedRepoNote.getCreatedAt())
                .expirationFrom(persistedRepoNote.getExpirationFrom())
                .expirationPeriod(persistedRepoNote.getExpirationPeriod())
                .build();
        noteCache.save(persistedCacheNote);

        Supplier<Optional<NoteDto>> requestAction = () -> {
            try {
                return Optional.of(noteService.getNote(new GetNoteRequestDto(REQUESTED_NOTE_URL)));
            } catch (NoteUnavailableException e) {
                return Optional.empty();
            }
        };

        List<CompletableFuture<Optional<NoteDto>>> futures = Stream.generate(
                        () -> CompletableFuture.supplyAsync(requestAction, executors)
                )
                .limit(CPU_COUNT)
                .toList();

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        List<Optional<NoteDto>> requestResults = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        long availableNotes = requestResults.stream().filter(Optional::isPresent).count();
        long unavailableNotes = requestResults.stream().filter(Optional::isEmpty).count();

        assertEquals(0, availableNotes);
        assertEquals(CPU_COUNT, unavailableNotes);

        NoteCacheable actualCacheNote = noteCache.get(REQUESTED_NOTE_URL).get();
        NoteCacheable expectedCacheNote = persistedCacheNote.toBuilder()
                .isAvailable(false)
                .build();

        assertEquals(expectedCacheNote, actualCacheNote);

        Note actualRepoNote = noteRepository.findByUrl(REQUESTED_NOTE_URL).get();
        Note expectedRepoNote = persistedRepoNote.toBuilder()
                .isAvailable(false)
                .build();

        assertEquals(expectedRepoNote, actualRepoNote);
    }

    @Test
    public void concurrentGetNoteWithNeverExpiration() {
        final String REQUESTED_NOTE_URL = "4";

        LocalDateTime createAt = LocalDateTime.now();

        Note persistedRepoNote = Note.builder()
                .url(REQUESTED_NOTE_URL)
                .isAvailable(true)
                .title("title")
                .content("content")
                .expirationType(ExpirationType.NEVER)
                .createdAt(createAt)
                .expirationFrom(null)
                .build();
        persistedRepoNote = noteRepository.save(persistedRepoNote);

        NoteCacheable persistedCacheNote = NoteCacheable.builder()
                .url(REQUESTED_NOTE_URL)
                .id(persistedRepoNote.getId())
                .isAvailable(persistedRepoNote.isAvailable())
                .title(persistedRepoNote.getTitle())
                .content(persistedRepoNote.getContent())
                .expirationType(persistedRepoNote.getExpirationType())
                .createdAt(persistedRepoNote.getCreatedAt())
                .expirationFrom(persistedRepoNote.getExpirationFrom())
                .build();
        noteCache.save(persistedCacheNote);

        Supplier<NoteDto> requestAction =
                () -> noteService.getNote(new GetNoteRequestDto(REQUESTED_NOTE_URL));

        List<CompletableFuture<NoteDto>> requestResults = Stream.generate(
                        () -> CompletableFuture.supplyAsync(requestAction, executors)
                )
                .limit(CPU_COUNT)
                .toList();

        CompletableFuture.allOf(requestResults.toArray(CompletableFuture[]::new)).join();
        List<NoteDto> actualNotes = requestResults.stream()
                .map(CompletableFuture::join)
                .toList();

        long actualCountNotes = actualNotes.size();
        assertEquals(CPU_COUNT, actualCountNotes);

        NoteDto expectedNote = NoteDto.builder()
                .id(persistedRepoNote.getId())
                .url(REQUESTED_NOTE_URL)
                .isAvailable(true)
                .title("title")
                .content("content")
                .expirationType(ExpirationType.NEVER)
                .createdAt(createAt)
                .expirationFrom(null)
                .build();

        actualNotes.forEach(n -> assertEquals(expectedNote, n));

        // times(1) due to storages initialization
        verify(noteCache, times(1)).save(persistedCacheNote);
        verify(noteRepository, times(1)).save(persistedRepoNote);

        verify(noteCache, times(CPU_COUNT)).getAndExpire(REQUESTED_NOTE_URL);
        verify(noteCache, never()).update(any(), any());
        verify(noteRepository, never()).findByUrl(REQUESTED_NOTE_URL);
    }

    @Test
    public void getNoteWithNonExistentNote() {
        final String REQUESTED_NOTE_URL = "5";

        assertThrows(NoteNonExistsException.class, () -> noteService.getNote(
                new GetNoteRequestDto(REQUESTED_NOTE_URL)
        ));

        verify(noteCache, times(1)).getAndExpire(REQUESTED_NOTE_URL);
        verify(noteRepository, times(1)).findByUrl(REQUESTED_NOTE_URL);
    }

    @Test
    public void getNotePersistedOnlyInRepository() {
        final String REQUESTED_NOTE_URL = "6";

        LocalDateTime createAt = LocalDateTime.now();

        Note persistedRepoNote = Note.builder()
                .url(REQUESTED_NOTE_URL)
                .isAvailable(true)
                .title("title")
                .content("content")
                .expirationType(ExpirationType.NEVER)
                .createdAt(createAt)
                .expirationFrom(null)
                .build();
        persistedRepoNote = noteRepository.save(persistedRepoNote);

        noteService.getNote(
                new GetNoteRequestDto(REQUESTED_NOTE_URL)
        );

        NoteCacheable expectedNoteCacheable = NoteCacheable.builder()
                .url(REQUESTED_NOTE_URL)
                .id(persistedRepoNote.getId())
                .isAvailable(persistedRepoNote.isAvailable())
                .title(persistedRepoNote.getTitle())
                .content(persistedRepoNote.getContent())
                .expirationType(persistedRepoNote.getExpirationType())
                .createdAt(persistedRepoNote.getCreatedAt())
                .expirationFrom(persistedRepoNote.getExpirationFrom())
                .build();

        NoteCacheable actualNoteCacheable = noteCache.get(REQUESTED_NOTE_URL).get();

        verify(noteCache, times(1)).save(expectedNoteCacheable);
        assertEquals(expectedNoteCacheable, actualNoteCacheable);

        noteService.getNote(new GetNoteRequestDto(REQUESTED_NOTE_URL));

        // only first time
        verify(noteRepository, times(1)).findByUrl(REQUESTED_NOTE_URL);
        verify(noteCache, times(2)).getAndExpire(REQUESTED_NOTE_URL);
    }
}
