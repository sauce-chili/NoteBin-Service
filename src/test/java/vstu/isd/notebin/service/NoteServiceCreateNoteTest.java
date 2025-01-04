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
import vstu.isd.notebin.dto.CreateNoteRequestDto;
import vstu.isd.notebin.dto.NoteDto;
import vstu.isd.notebin.entity.ExpirationType;
import vstu.isd.notebin.exception.*;
import vstu.isd.notebin.repository.NoteRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static vstu.isd.notebin.testutils.TestAsserts.*;

@SpringBootTest
@ContextConfiguration(initializers = TestContainersConfig.class)
@Slf4j
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class NoteServiceCreateNoteTest {

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
     * Tests for NoteService.createNote(...)
     *
     * Aspects of testing:
     * - creating note with different expiration type: NEVER, BURN_BY_PERIOD, BURN_AFTER_READ
     * - validation errors
     * - creating few same notes, few different notes
     * - concurrency while creating
     * */
    @Test
    public void createNote() {

        LocalDateTime now = LocalDateTime.now();
        Duration expirationPeriod = Duration.ofMinutes(15);
        String title = "New note";
        String content = "My content";
        CreateNoteRequestDto createNoteRequestDto = CreateNoteRequestDto.builder()
                .title(title)
                .content(content)
                .expirationType(ExpirationType.NEVER)
                .expirationPeriod(expirationPeriod)
                .build();

        NoteDto actualCreatedNoteDto = noteService.createNote(createNoteRequestDto);

        NoteDto expectedCreatedNoteDto = NoteDto.builder()
                .id(actualCreatedNoteDto.getId())
                .url(actualCreatedNoteDto.getUrl())
                .isAvailable(true)
                .title(title)
                .content(content)
                .createdAt(now)
                .expirationType(ExpirationType.NEVER)
                .expirationFrom(now)
                .expirationPeriod(expirationPeriod)
                .build();
        assertNotNull(actualCreatedNoteDto.getId());
        assertNotNull(actualCreatedNoteDto.getUrl());
        assertNoteDtoEquals(expectedCreatedNoteDto, actualCreatedNoteDto);
        assertNoteExistsInRepository(actualCreatedNoteDto, noteRepository);
        assertNoteExistsInCache(actualCreatedNoteDto, noteCache);
    }

    @Test
    public void createTwoSameNotes() {

        LocalDateTime now = LocalDateTime.now();
        Duration expirationPeriod = Duration.ofMinutes(15);
        String title = "New note";
        String content = "My content";
        CreateNoteRequestDto createNoteRequestDto = CreateNoteRequestDto.builder()
                .title(title)
                .content(content)
                .expirationType(ExpirationType.NEVER)
                .expirationPeriod(expirationPeriod)
                .build();


        NoteDto actualCreatedFirstNoteDto = noteService.createNote(createNoteRequestDto);
        NoteDto actualCreatedSecondNoteDto = noteService.createNote(createNoteRequestDto);

        String expUrlOfFirstNote = "1";
        NoteDto expectedCreatedFirstNoteDto = NoteDto.builder()
                .id(1L)
                .url(expUrlOfFirstNote)
                .isAvailable(true)
                .title(title)
                .content(content)
                .createdAt(now)
                .expirationType(ExpirationType.NEVER)
                .expirationFrom(now)
                .expirationPeriod(expirationPeriod)
                .build();
        assertNoteDtoEquals(expectedCreatedFirstNoteDto, actualCreatedFirstNoteDto);
        assertNoteExistsInRepository(actualCreatedFirstNoteDto, noteRepository);
        assertNoteExistsInCache(actualCreatedFirstNoteDto, noteCache);

        String expUrlOfSecondNote = "2";
        NoteDto expectedCreatedSecondNoteDto = NoteDto.builder()
                .id(2L)
                .url(expUrlOfSecondNote)
                .isAvailable(true)
                .title(title)
                .content(content)
                .createdAt(now)
                .expirationType(ExpirationType.NEVER)
                .expirationFrom(now)
                .expirationPeriod(expirationPeriod)
                .build();
        assertNoteDtoEquals(expectedCreatedSecondNoteDto, actualCreatedSecondNoteDto);
        assertNoteExistsInRepository(actualCreatedSecondNoteDto, noteRepository);
        assertNoteExistsInCache(actualCreatedSecondNoteDto, noteCache);
    }

    @Test
    public void createTwoDifferentNotes() {

        LocalDateTime now = LocalDateTime.now();
        Duration expirationPeriod = Duration.ofMinutes(15);
        String title = "New note";
        String content = "My content";
        CreateNoteRequestDto createNoteRequestDtoFirst = CreateNoteRequestDto.builder()
                .title(title)
                .content(content)
                .expirationType(ExpirationType.NEVER)
                .expirationPeriod(expirationPeriod)
                .build();
        NoteDto actualCreatedFirstNoteDto = noteService.createNote(createNoteRequestDtoFirst);

        LocalDateTime nowSecond = LocalDateTime.now();
        Duration expirationPeriodSecond = Duration.ofMinutes(15);
        String titleSecond = "New note";
        String contentSecond = "My content";
        CreateNoteRequestDto createNoteRequestDtoSecond = CreateNoteRequestDto.builder()
                .title(titleSecond)
                .content(contentSecond)
                .expirationType(ExpirationType.NEVER)
                .expirationPeriod(expirationPeriodSecond)
                .build();
        NoteDto actualCreatedSecondNoteDto = noteService.createNote(createNoteRequestDtoSecond);

        String expUrlOfFirstNote = "1";
        NoteDto expectedCreatedFirstNoteDto = NoteDto.builder()
                .id(1L)
                .url(expUrlOfFirstNote)
                .isAvailable(true)
                .title(title)
                .content(content)
                .createdAt(now)
                .expirationType(ExpirationType.NEVER)
                .expirationFrom(now)
                .expirationPeriod(expirationPeriod)
                .build();
        assertNoteDtoEquals(expectedCreatedFirstNoteDto, actualCreatedFirstNoteDto);
        assertNoteExistsInRepository(actualCreatedFirstNoteDto, noteRepository);
        assertNoteExistsInCache(actualCreatedFirstNoteDto, noteCache);

        String expUrlOfSecondNote = "2";
        NoteDto expectedCreatedSecondNoteDto = NoteDto.builder()
                .id(2L)
                .url(expUrlOfSecondNote)
                .isAvailable(true)
                .title(titleSecond)
                .content(contentSecond)
                .createdAt(nowSecond)
                .expirationType(ExpirationType.NEVER)
                .expirationFrom(nowSecond)
                .expirationPeriod(expirationPeriod)
                .build();
        assertNoteDtoEquals(expectedCreatedSecondNoteDto, actualCreatedSecondNoteDto);
        assertNoteExistsInRepository(actualCreatedSecondNoteDto, noteRepository);
        assertNoteExistsInCache(actualCreatedSecondNoteDto, noteCache);
    }

    @Test
    public void createTwoConcurrentNotes() {

        Duration expirationPeriod = Duration.ofMinutes(15);
        String title = "New note";
        String content = "My content";
        CreateNoteRequestDto createNoteRequestDtoFirst = CreateNoteRequestDto.builder()
                .title(title)
                .content(content)
                .expirationType(ExpirationType.NEVER)
                .expirationPeriod(expirationPeriod)
                .build();

        Duration expirationPeriodSecond = Duration.ofMinutes(15);
        String titleSecond = "New note 2";
        String contentSecond = "My content 2";
        CreateNoteRequestDto createNoteRequestDtoSecond = CreateNoteRequestDto.builder()
                .title(titleSecond)
                .content(contentSecond)
                .expirationType(ExpirationType.NEVER)
                .expirationPeriod(expirationPeriodSecond)
                .build();


        CompletableFuture<NoteDto> futureNoteFirst = CompletableFuture.supplyAsync(
                () -> noteService.createNote(createNoteRequestDtoFirst), executors);

        CompletableFuture<NoteDto> futureNoteSecond = CompletableFuture.supplyAsync(
                () -> noteService.createNote(createNoteRequestDtoSecond), executors);

        CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(futureNoteFirst, futureNoteSecond);
        combinedFuture.join();

        NoteDto actualCreatedFirstNoteDto = futureNoteFirst.join();
        NoteDto actualCreatedSecondNoteDto = futureNoteSecond.join();


        assertNoteExistsInRepository(actualCreatedFirstNoteDto, noteRepository);
        assertNoteExistsInCache(actualCreatedFirstNoteDto, noteCache);

        assertNoteExistsInRepository(actualCreatedSecondNoteDto, noteRepository);
        assertNoteExistsInCache(actualCreatedSecondNoteDto, noteCache);
    }

    // invalid title ----------------------------------------------------------------------------------
    @Test
    void titleIsNull(){

        Duration expirationPeriod = Duration.ofMinutes(15);
        String content = "My content";
        CreateNoteRequestDto createNoteRequestDto = CreateNoteRequestDto.builder()
                .title(null)
                .content(content)
                .expirationType(ExpirationType.NEVER)
                .expirationPeriod(expirationPeriod)
                .build();

        GroupValidationException groupOfExceptions = assertThrows(
                GroupValidationException.class,
                () -> {
                    noteService.createNote(createNoteRequestDto);
                }
        );

        List<? extends ValidationException> exceptions = groupOfExceptions.getExceptions();

        assertEquals(1, exceptions.size());
        assertEquals(ClientExceptionName.INVALID_TITLE, exceptions.get(0).getExceptionName());
    }

    @Test
    void titleIsInvalid(){

        Duration expirationPeriod = Duration.ofMinutes(15);
        String title = ",Title";
        String content = "My content";
        CreateNoteRequestDto createNoteRequestDto = CreateNoteRequestDto.builder()
                .title(title)
                .content(content)
                .expirationType(ExpirationType.NEVER)
                .expirationPeriod(expirationPeriod)
                .build();

        GroupValidationException groupOfExceptions = assertThrows(
                GroupValidationException.class,
                () -> {
                    noteService.createNote(createNoteRequestDto);
                }
        );

        List<? extends ValidationException> exceptions = groupOfExceptions.getExceptions();

        assertEquals(1, exceptions.size());
        assertEquals(ClientExceptionName.INVALID_TITLE, exceptions.get(0).getExceptionName());
    }

    @Test
    void titleLengthIsLargerThanMaxLength(){

        Duration expirationPeriod = Duration.ofMinutes(15);
        String title = "snBkSFkghmcmCBvWDksdGfnIJdxvkqEergXjqfbsDhiAgUjMKVjXOXSgpaqkkWLlMFREzvkPgRXvVnDKvixysCCUGMhHzwqBnxqZkkDMKDnhaltnKyXgLuQagrZxNSFbhM";
        String content = "My content";
        CreateNoteRequestDto createNoteRequestDto = CreateNoteRequestDto.builder()
                .title(title)
                .content(content)
                .expirationType(ExpirationType.NEVER)
                .expirationPeriod(expirationPeriod)
                .build();

        GroupValidationException groupOfExceptions = assertThrows(
                GroupValidationException.class,
                () -> {
                    noteService.createNote(createNoteRequestDto);
                }
        );

        List<? extends ValidationException> exceptions = groupOfExceptions.getExceptions();

        assertEquals(1, exceptions.size());
        assertEquals(ClientExceptionName.INVALID_TITLE, exceptions.get(0).getExceptionName());
    }

    // invalid content --------------------------------------------------------------------------------
    @Test
    void contentIsNull(){

        Duration expirationPeriod = Duration.ofMinutes(15);
        String title = "Title";
        String content = null;
        CreateNoteRequestDto createNoteRequestDto = CreateNoteRequestDto.builder()
                .title(title)
                .content(content)
                .expirationType(ExpirationType.NEVER)
                .expirationPeriod(expirationPeriod)
                .build();

        GroupValidationException groupOfExceptions = assertThrows(
                GroupValidationException.class,
                () -> {
                    noteService.createNote(createNoteRequestDto);
                }
        );

        List<? extends ValidationException> exceptions = groupOfExceptions.getExceptions();

        assertEquals(1, exceptions.size());
        assertEquals(ClientExceptionName.INVALID_CONTENT, exceptions.get(0).getExceptionName());
    }

    @Test
    void contentIsInvalid(){

        Duration expirationPeriod = Duration.ofMinutes(15);
        String title = "Title";
        String content = ",.,., . .,. ";
        CreateNoteRequestDto createNoteRequestDto = CreateNoteRequestDto.builder()
                .title(title)
                .content(content)
                .expirationType(ExpirationType.NEVER)
                .expirationPeriod(expirationPeriod)
                .build();

        GroupValidationException groupOfExceptions = assertThrows(
                GroupValidationException.class,
                () -> {
                    noteService.createNote(createNoteRequestDto);
                }
        );

        List<? extends ValidationException> exceptions = groupOfExceptions.getExceptions();

        assertEquals(1, exceptions.size());
        assertEquals(ClientExceptionName.INVALID_CONTENT, exceptions.get(0).getExceptionName());
    }

    // invalid expiration type ------------------------------------------------------------------------
    @Test
    void expirationTypeIsNull(){

        Duration expirationPeriod = Duration.ofMinutes(15);
        String title = "Title";
        String content = "My content";
        CreateNoteRequestDto createNoteRequestDto = CreateNoteRequestDto.builder()
                .title(title)
                .content(content)
                .expirationType(null)
                .expirationPeriod(expirationPeriod)
                .build();

        GroupValidationException groupOfExceptions = assertThrows(
                GroupValidationException.class,
                () -> {
                    noteService.createNote(createNoteRequestDto);
                }
        );

        List<? extends ValidationException> exceptions = groupOfExceptions.getExceptions();

        assertEquals(1, exceptions.size());
        assertEquals(ClientExceptionName.INVALID_EXPIRATION_TYPE, exceptions.get(0).getExceptionName());
    }

    // invalid expiration period ----------------------------------------------------------------------
    @Test
    void expirationPeriodIsNull(){

        Duration expirationPeriod = null;
        String title = "Title";
        String content = "My content";
        CreateNoteRequestDto createNoteRequestDto = CreateNoteRequestDto.builder()
                .title(title)
                .content(content)
                .expirationType(ExpirationType.NEVER)
                .expirationPeriod(expirationPeriod)
                .build();

        GroupValidationException groupOfExceptions = assertThrows(
                GroupValidationException.class,
                () -> {
                    noteService.createNote(createNoteRequestDto);
                }
        );

        List<? extends ValidationException> exceptions = groupOfExceptions.getExceptions();

        assertEquals(1, exceptions.size());
        assertEquals(ClientExceptionName.INVALID_EXPIRATION_PERIOD, exceptions.get(0).getExceptionName());
    }

    // invalid all fields ----------------------------------------------------------------------------
    @Test
    void allFieldsAreInvalid(){

        Duration expirationPeriod = null;
        String title = ",Title";
        String content = "  ., ";
        CreateNoteRequestDto createNoteRequestDto = CreateNoteRequestDto.builder()
                .title(title)
                .content(content)
                .expirationType(null)
                .expirationPeriod(expirationPeriod)
                .build();

        GroupValidationException groupOfExceptions = assertThrows(
                GroupValidationException.class,
                () -> {
                    noteService.createNote(createNoteRequestDto);
                }
        );

        List<? extends ValidationException> exceptions = groupOfExceptions.getExceptions();

        assertEquals(4, exceptions.size());
        assertEquals(ClientExceptionName.INVALID_TITLE, exceptions.get(0).getExceptionName());
        assertEquals(ClientExceptionName.INVALID_CONTENT, exceptions.get(1).getExceptionName());
        assertEquals(ClientExceptionName.INVALID_EXPIRATION_TYPE, exceptions.get(2).getExceptionName());
        assertEquals(ClientExceptionName.INVALID_EXPIRATION_PERIOD, exceptions.get(3).getExceptionName());
    }
}