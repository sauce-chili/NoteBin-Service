package vstu.isd.notebin.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import vstu.isd.notebin.cache.NoteCache;
import vstu.isd.notebin.config.TestContainersConfig;
import vstu.isd.notebin.dto.CreateNoteRequestDto;
import vstu.isd.notebin.dto.NoteDto;
import vstu.isd.notebin.entity.ExpirationType;
import vstu.isd.notebin.exception.*;
import vstu.isd.notebin.mapper.NoteMapper;
import vstu.isd.notebin.repository.NoteRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static vstu.isd.notebin.testutils.TestAsserts.*;

@SpringBootTest
@ContextConfiguration(initializers = TestContainersConfig.class)
@Slf4j
public class NoteServiceCreateNoteTest {

    @SpyBean
    private NoteRepository noteRepository;
    @SpyBean
    private NoteCache noteCache;
    @Autowired
    private NoteService noteService;
    @Autowired
    private int contentLength;
    @Autowired
    private NoteMapper noteMapper;

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;

    private final static ExecutorService executors = Executors.newFixedThreadPool(MAXIMUM_POOL_SIZE);

    /**
     * Generates string specified length.
     *
     * @param length length
     * @return string specified length.
     */
    String stringOfLength(int length){

        return "a".repeat(Math.max(0, length));
    }

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
        String title = "New note";
        String content = "My content";
        CreateNoteRequestDto createNoteRequestDto = CreateNoteRequestDto.builder()
                .title(title)
                .content(content)
                .expirationType(ExpirationType.NEVER)
                .expirationPeriod(null)
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
                .expirationFrom(null)
                .expirationPeriod(null)
                .build();
        assertNotNull(actualCreatedNoteDto.getId());
        assertNotNull(actualCreatedNoteDto.getUrl());
        assertNoteDtoEquals(expectedCreatedNoteDto, actualCreatedNoteDto);
        NoteDto actualNoteInRepos = noteMapper.toDto(noteRepository.findByUrl(actualCreatedNoteDto.getUrl()).get());
        NoteDto actualNoteInCache = noteMapper.toDto(noteCache.get(actualCreatedNoteDto.getUrl()).get());
        assertNoteDtoEquals(expectedCreatedNoteDto, actualNoteInRepos);
        assertNoteDtoEquals(expectedCreatedNoteDto, actualNoteInCache);
    }

    @Test
    public void createTwoSameNotes() {

        LocalDateTime now = LocalDateTime.now();
        Duration expirationPeriod = null;
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


        NoteDto expectedCreatedFirstNoteDto = NoteDto.builder()
                .id(actualCreatedFirstNoteDto.getId())
                .url(actualCreatedFirstNoteDto.getUrl())
                .isAvailable(true)
                .title(title)
                .content(content)
                .createdAt(now)
                .expirationType(ExpirationType.NEVER)
                .expirationFrom(null)
                .expirationPeriod(expirationPeriod)
                .build();
        assertNoteDtoEquals(expectedCreatedFirstNoteDto, actualCreatedFirstNoteDto);
        NoteDto actualFirstNoteInRepos = noteMapper.toDto(noteRepository.findByUrl(actualCreatedFirstNoteDto.getUrl()).get());
        NoteDto actualFirstNoteInCache = noteMapper.toDto(noteCache.get(actualCreatedFirstNoteDto.getUrl()).get());
        assertNoteDtoEquals(expectedCreatedFirstNoteDto, actualFirstNoteInRepos);
        assertNoteDtoEquals(expectedCreatedFirstNoteDto, actualFirstNoteInCache);

        NoteDto expectedCreatedSecondNoteDto = NoteDto.builder()
                .id(actualCreatedSecondNoteDto.getId())
                .url(actualCreatedSecondNoteDto.getUrl())
                .isAvailable(true)
                .title(title)
                .content(content)
                .createdAt(now)
                .expirationType(ExpirationType.NEVER)
                .expirationFrom(null)
                .expirationPeriod(expirationPeriod)
                .build();
        assertNoteDtoEquals(expectedCreatedSecondNoteDto, actualCreatedSecondNoteDto);
        NoteDto actualSecondNoteInRepos = noteMapper.toDto(noteRepository.findByUrl(actualCreatedSecondNoteDto.getUrl()).get());
        NoteDto actualSecondNoteInCache = noteMapper.toDto(noteCache.get(actualCreatedSecondNoteDto.getUrl()).get());
        assertNoteDtoEquals(expectedCreatedSecondNoteDto, actualSecondNoteInRepos);
        assertNoteDtoEquals(expectedCreatedSecondNoteDto, actualSecondNoteInCache);
    }

    @Test
    public void createTwoDifferentNotes() {

        LocalDateTime now = LocalDateTime.now();
        Duration expirationPeriod = null;
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
        Duration expirationPeriodSecond = Duration.ofMinutes(20);
        String titleSecond = "Another note";
        String contentSecond = "Another content of my note";
        CreateNoteRequestDto createNoteRequestDtoSecond = CreateNoteRequestDto.builder()
                .title(titleSecond)
                .content(contentSecond)
                .expirationType(ExpirationType.BURN_BY_PERIOD)
                .expirationPeriod(expirationPeriodSecond)
                .build();
        NoteDto actualCreatedSecondNoteDto = noteService.createNote(createNoteRequestDtoSecond);

        NoteDto expectedCreatedFirstNoteDto = NoteDto.builder()
                .id(actualCreatedFirstNoteDto.getId())
                .url(actualCreatedFirstNoteDto.getUrl())
                .isAvailable(true)
                .title(title)
                .content(content)
                .createdAt(now)
                .expirationType(ExpirationType.NEVER)
                .expirationFrom(null)
                .expirationPeriod(expirationPeriod)
                .build();
        assertNoteDtoEquals(expectedCreatedFirstNoteDto, actualCreatedFirstNoteDto);
        NoteDto actualFirstNoteInRepos = noteMapper.toDto(noteRepository.findByUrl(actualCreatedFirstNoteDto.getUrl()).get());
        NoteDto actualFirstNoteInCache = noteMapper.toDto(noteCache.get(actualCreatedFirstNoteDto.getUrl()).get());
        assertNoteDtoEquals(expectedCreatedFirstNoteDto, actualFirstNoteInRepos);
        assertNoteDtoEquals(expectedCreatedFirstNoteDto, actualFirstNoteInCache);

        NoteDto expectedCreatedSecondNoteDto = NoteDto.builder()
                .id(actualCreatedSecondNoteDto.getId())
                .url(actualCreatedSecondNoteDto.getUrl())
                .isAvailable(true)
                .title(titleSecond)
                .content(contentSecond)
                .createdAt(nowSecond)
                .expirationType(ExpirationType.BURN_BY_PERIOD)
                .expirationFrom(nowSecond)
                .expirationPeriod(expirationPeriodSecond)
                .build();
        assertNoteDtoEquals(expectedCreatedSecondNoteDto, actualCreatedSecondNoteDto);
        NoteDto actualSecondNoteInRepos = noteMapper.toDto(noteRepository.findByUrl(actualCreatedSecondNoteDto.getUrl()).get());
        NoteDto actualSecondNoteInCache = noteMapper.toDto(noteCache.get(actualCreatedSecondNoteDto.getUrl()).get());
        assertNoteDtoEquals(expectedCreatedSecondNoteDto, actualSecondNoteInRepos);
        assertNoteDtoEquals(expectedCreatedSecondNoteDto, actualSecondNoteInCache);
    }

    @Test
    public void createTwoConcurrentNotes() {

        Duration expirationPeriod = null;
        String title = "New note";
        String content = "My content";
        CreateNoteRequestDto createNoteRequestDtoFirst = CreateNoteRequestDto.builder()
                .title(title)
                .content(content)
                .expirationType(ExpirationType.NEVER)
                .expirationPeriod(expirationPeriod)
                .build();

        Duration expirationPeriodSecond = null;
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


        NoteDto expectedCreatedFirstNoteDto = NoteDto.builder()
                .id(actualCreatedFirstNoteDto.getId())
                .url(actualCreatedFirstNoteDto.getUrl())
                .isAvailable(true)
                .title(title)
                .content(content)
                .createdAt(LocalDateTime.now())
                .expirationType(ExpirationType.NEVER)
                .expirationFrom(null)
                .expirationPeriod(expirationPeriod)
                .build();

        NoteDto expectedCreatedSecondNoteDto = NoteDto.builder()
                .id(actualCreatedSecondNoteDto.getId())
                .url(actualCreatedSecondNoteDto.getUrl())
                .isAvailable(true)
                .title(titleSecond)
                .content(contentSecond)
                .createdAt(LocalDateTime.now())
                .expirationType(ExpirationType.NEVER)
                .expirationFrom(null)
                .expirationPeriod(expirationPeriodSecond)
                .build();

        NoteDto actualNoteInRepos = noteMapper.toDto(noteRepository.findByUrl(actualCreatedSecondNoteDto.getUrl()).get());
        NoteDto actualNoteInCache = noteMapper.toDto(noteCache.get(actualCreatedSecondNoteDto.getUrl()).get());

        boolean firstPassed = false;
        try {
            assertNoteDtoEquals(expectedCreatedFirstNoteDto, actualCreatedFirstNoteDto);
            assertNoteDtoEquals(expectedCreatedFirstNoteDto, actualNoteInRepos);
            assertNoteDtoEquals(expectedCreatedFirstNoteDto, actualNoteInCache);
            firstPassed = true;
        } catch (AssertionError ignored) {
        }

        if (!firstPassed) {
            assertNoteDtoEquals(expectedCreatedSecondNoteDto, actualCreatedSecondNoteDto);
            assertNoteDtoEquals(expectedCreatedSecondNoteDto, actualNoteInRepos);
            assertNoteDtoEquals(expectedCreatedSecondNoteDto, actualNoteInCache);
        }
    }

    // invalid title ----------------------------------------------------------------------------------
    @Test
    void titleIsNull(){

        Duration expirationPeriod = null;
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


        List<ClientExceptionName> expected = List.of(
                ClientExceptionName.INVALID_TITLE
        );
        List<ClientExceptionName> actual = groupOfExceptions.getExceptions().stream()
                .map(ValidationException::getExceptionName)
                .sorted(Comparator.comparing(Enum::ordinal))
                .toList();
        assertEquals(expected.size(), actual.size());
        assertEquals(expected, actual);
    }

    @Test
    void titleIsInvalid(){

        Duration expirationPeriod = null;
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


        List<ClientExceptionName> expected = List.of(
                ClientExceptionName.INVALID_TITLE
        );
        List<ClientExceptionName> actual = groupOfExceptions.getExceptions().stream()
                .map(ValidationException::getExceptionName)
                .sorted(Comparator.comparing(Enum::ordinal))
                .toList();
        assertEquals(expected.size(), actual.size());
        assertEquals(expected, actual);
    }

    @Test
    void titleLengthIsLargerThanMaxLength(){

        Duration expirationPeriod = null;
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


        List<ClientExceptionName> expected = List.of(
                ClientExceptionName.INVALID_TITLE
        );
        List<ClientExceptionName> actual = groupOfExceptions.getExceptions().stream()
                .map(ValidationException::getExceptionName)
                .sorted(Comparator.comparing(Enum::ordinal))
                .toList();
        assertEquals(expected.size(), actual.size());
        assertEquals(expected, actual);
    }

    // invalid content --------------------------------------------------------------------------------
    @Test
    void contentIsNull(){

        Duration expirationPeriod = null;
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


        List<ClientExceptionName> expected = List.of(
                ClientExceptionName.INVALID_CONTENT
        );
        List<ClientExceptionName> actual = groupOfExceptions.getExceptions().stream()
                .map(ValidationException::getExceptionName)
                .sorted(Comparator.comparing(Enum::ordinal))
                .toList();
        assertEquals(expected.size(), actual.size());
        assertEquals(expected, actual);
    }

    @Test
    void contentIsInvalid(){

        Duration expirationPeriod = null;
        String title = "Title";
        String content = stringOfLength(contentLength + 1);
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


        List<ClientExceptionName> expected = List.of(
                ClientExceptionName.INVALID_CONTENT
        );
        List<ClientExceptionName> actual = groupOfExceptions.getExceptions().stream()
                .map(ValidationException::getExceptionName)
                .sorted(Comparator.comparing(Enum::ordinal))
                .toList();
        assertEquals(expected.size(), actual.size());
        assertEquals(expected, actual);
    }

    // invalid expiration type ------------------------------------------------------------------------
    @Test
    void expirationTypeIsNull(){

        Duration expirationPeriod = null;
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


        List<ClientExceptionName> expected = List.of(
                ClientExceptionName.INVALID_EXPIRATION_TYPE
        );
        List<ClientExceptionName> actual = groupOfExceptions.getExceptions().stream()
                .map(ValidationException::getExceptionName)
                .sorted(Comparator.comparing(Enum::ordinal))
                .toList();
        assertEquals(expected.size(), actual.size());
        assertEquals(expected, actual);
    }

    // invalid expiration period ----------------------------------------------------------------------

    @Test
    void expirationPeriodNotSetWhileExpirationTypeIsBurnAfterRead(){

        LocalDateTime now = LocalDateTime.now();
        String title = "New note";
        String content = "My content";
        CreateNoteRequestDto createNoteRequestDto = CreateNoteRequestDto.builder()
                .title(title)
                .content(content)
                .expirationType(ExpirationType.BURN_AFTER_READ)
                .expirationPeriod(null)
                .build();


        NoteDto actualCreatedNoteDto = noteService.createNote(createNoteRequestDto);


        NoteDto expectedCreatedNoteDto = NoteDto.builder()
                .id(actualCreatedNoteDto.getId())
                .url(actualCreatedNoteDto.getUrl())
                .isAvailable(true)
                .title(title)
                .content(content)
                .createdAt(now)
                .expirationType(ExpirationType.BURN_AFTER_READ)
                .expirationFrom(null)
                .expirationPeriod(null)
                .build();
        assertNotNull(actualCreatedNoteDto.getId());
        assertNotNull(actualCreatedNoteDto.getUrl());
        assertNoteDtoEquals(expectedCreatedNoteDto, actualCreatedNoteDto);
        NoteDto actualNoteInRepos = noteMapper.toDto(noteRepository.findByUrl(actualCreatedNoteDto.getUrl()).get());
        NoteDto actualNoteInCache = noteMapper.toDto(noteCache.get(actualCreatedNoteDto.getUrl()).get());
        assertNoteDtoEquals(expectedCreatedNoteDto, actualNoteInRepos);
        assertNoteDtoEquals(expectedCreatedNoteDto, actualNoteInCache);
    }

    @Test
    void expirationPeriodSetWhileExpirationTypeIsBurnByPeriod(){

        LocalDateTime now = LocalDateTime.now();
        String title = "New note";
        String content = "My content";
        Duration expirationPeriod = Duration.ofMinutes(15);
        CreateNoteRequestDto createNoteRequestDto = CreateNoteRequestDto.builder()
                .title(title)
                .content(content)
                .expirationType(ExpirationType.BURN_BY_PERIOD)
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
                .expirationType(ExpirationType.BURN_BY_PERIOD)
                .expirationFrom(now)
                .expirationPeriod(expirationPeriod)
                .build();
        assertNotNull(actualCreatedNoteDto.getId());
        assertNotNull(actualCreatedNoteDto.getUrl());
        assertNoteDtoEquals(expectedCreatedNoteDto, actualCreatedNoteDto);
        NoteDto actualNoteInRepos = noteMapper.toDto(noteRepository.findByUrl(actualCreatedNoteDto.getUrl()).get());
        NoteDto actualNoteInCache = noteMapper.toDto(noteCache.get(actualCreatedNoteDto.getUrl()).get());
        assertNoteDtoEquals(expectedCreatedNoteDto, actualNoteInRepos);
        assertNoteDtoEquals(expectedCreatedNoteDto, actualNoteInCache);
    }

    @Test
    void expirationPeriodNotSetWhenExpirationTypeIsBurnByPeriod(){

        String title = "New note";
        String content = "My content";
        Duration expirationPeriod = null;
        CreateNoteRequestDto createNoteRequestDto = CreateNoteRequestDto.builder()
                .title(title)
                .content(content)
                .expirationType(ExpirationType.BURN_BY_PERIOD)
                .expirationPeriod(expirationPeriod)
                .build();


        GroupValidationException groupOfExceptions = assertThrows(
                GroupValidationException.class,
                () -> {
                    noteService.createNote(createNoteRequestDto);
                }
        );


        List<ClientExceptionName> expected = List.of(
                ClientExceptionName.INVALID_EXPIRATION_PERIOD
        );
        List<ClientExceptionName> actual = groupOfExceptions.getExceptions().stream()
                .map(ValidationException::getExceptionName)
                .sorted(Comparator.comparing(Enum::ordinal))
                .toList();
        assertEquals(expected.size(), actual.size());
        assertEquals(expected, actual);
    }

    @Test
    void expirationPeriodSetWhenExpirationTypeIsNever(){

        String title = "New note";
        String content = "My content";
        Duration expirationPeriod = Duration.ofMinutes(15);
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


        List<ClientExceptionName> expected = List.of(
                ClientExceptionName.INVALID_EXPIRATION_PERIOD
        );
        List<ClientExceptionName> actual = groupOfExceptions.getExceptions().stream()
                .map(ValidationException::getExceptionName)
                .sorted(Comparator.comparing(Enum::ordinal))
                .toList();
        assertEquals(expected.size(), actual.size());
        assertEquals(expected, actual);
    }

    @Test
    void expirationPeriodSetWhenExpirationTypeIsBurnAfterRead(){

        String title = "New note";
        String content = "My content";
        Duration expirationPeriod = Duration.ofMinutes(15);
        CreateNoteRequestDto createNoteRequestDto = CreateNoteRequestDto.builder()
                .title(title)
                .content(content)
                .expirationType(ExpirationType.BURN_AFTER_READ)
                .expirationPeriod(expirationPeriod)
                .build();


        GroupValidationException groupOfExceptions = assertThrows(
                GroupValidationException.class,
                () -> {
                    noteService.createNote(createNoteRequestDto);
                }
        );


        List<ClientExceptionName> expected = List.of(
                ClientExceptionName.INVALID_EXPIRATION_PERIOD
        );
        List<ClientExceptionName> actual = groupOfExceptions.getExceptions().stream()
                .map(ValidationException::getExceptionName)
                .sorted(Comparator.comparing(Enum::ordinal))
                .toList();
        assertEquals(expected.size(), actual.size());
        assertEquals(expected, actual);
    }

    // invalid all fields ----------------------------------------------------------------------------
    @Test
    void allFieldsAreInvalid(){

        Duration expirationPeriod = null;
        String title = ",Title";
        String content = stringOfLength(contentLength + 1);
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


        List<ClientExceptionName> expected = List.of(
                ClientExceptionName.INVALID_TITLE,
                ClientExceptionName.INVALID_CONTENT,
                ClientExceptionName.INVALID_EXPIRATION_TYPE
        );
        List<ClientExceptionName> actual = groupOfExceptions.getExceptions().stream()
                .map(ValidationException::getExceptionName)
                .sorted(Comparator.comparing(Enum::ordinal))
                .toList();
        assertEquals(expected.size(), actual.size());
        assertEquals(expected, actual);
    }
}