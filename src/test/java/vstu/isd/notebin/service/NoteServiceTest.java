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
import vstu.isd.notebin.dto.*;
import vstu.isd.notebin.entity.ExpirationType;
import vstu.isd.notebin.entity.Note;
import vstu.isd.notebin.entity.NoteCacheable;
import vstu.isd.notebin.exception.*;
import vstu.isd.notebin.mapper.NoteMapper;
import vstu.isd.notebin.repository.NoteRepository;
import vstu.isd.notebin.dto.CreateNoteRequestDto;
import vstu.isd.notebin.dto.NoteDto;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static vstu.isd.notebin.testutils.TestAsserts.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ContextConfiguration(initializers = TestContainersConfig.class)
@Slf4j
public class NoteServiceTest {

    @SpyBean
    private NoteRepository noteRepository;
    @SpyBean
    private NoteCache noteCache;
    @Autowired
    private NoteService noteService;
    @Autowired
    private NoteMapper noteMapper;
    @Autowired
    private int contentLength;

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;

    private final static ExecutorService executors = Executors.newFixedThreadPool(MAXIMUM_POOL_SIZE);

    /**
     * Generates string specified length.
     *
     * @param length length
     * @return string specified length.
     */
    private String stringOfLength(int length) {
        return "a".repeat(Math.max(0, length));
    }

    @Nested
    @DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
    class CreateNoteTest {

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

    // -----------------------------------------------------------------------------------------------------------------

    NoteDto generateNoteToRepos(){

        String title = "New note";
        String content = "My content";
        CreateNoteRequestDto createNoteRequestDto = CreateNoteRequestDto.builder()
                .title(title)
                .content(content)
                .expirationType(ExpirationType.NEVER)
                .expirationPeriod(null)
                .build();

        return noteService.createNote(createNoteRequestDto);
    }

    NoteDto generateNoteToReposWithExpTypeBurnAfterRead(){

        String title = "New note";
        String content = "My content";
        CreateNoteRequestDto createNoteRequestDto = CreateNoteRequestDto.builder()
                .title(title)
                .content(content)
                .expirationType(ExpirationType.BURN_AFTER_READ)
                .expirationPeriod(null)
                .build();

        return noteService.createNote(createNoteRequestDto);
    }

    NoteDto generateNoteToReposWithExpTypeBurnByPeriod(){

        String title = "New note";
        String content = "My content";
        Duration expirationPeriod = Duration.ofMinutes(15);
        CreateNoteRequestDto createNoteRequestDto = CreateNoteRequestDto.builder()
                .title(title)
                .content(content)
                .expirationType(ExpirationType.BURN_BY_PERIOD)
                .expirationPeriod(expirationPeriod)
                .build();

        return noteService.createNote(createNoteRequestDto);
    }

    List<NoteDto> generateNotesToRepos(int count){

        String defaultTitle = "My title";
        String defaultContent = "My content";
        ExpirationType expirationType = ExpirationType.NEVER;
        List<NoteDto> notes = new LinkedList<>();
        for (int i = 0; i < count; i++) {

            CreateNoteRequestDto createNoteRequestDto = CreateNoteRequestDto.builder()
                    .title(defaultTitle + " " + i)
                    .content(defaultContent + " " + i)
                    .expirationType(expirationType)
                    .expirationPeriod(null)
                    .build();

            notes.add(noteService.createNote(createNoteRequestDto));
        }

        return notes;
    }

    // -----------------------------------------------------------------------------------------------------------------

    @Nested
    @DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
    class UpdateNoteTest {

        @Test
        void updateNote(){

            NoteDto noteInReposBeforeUpdate = generateNoteToRepos();

            Duration expirationPeriod = Duration.ofMinutes(37);
            String url = noteInReposBeforeUpdate.getUrl();
            UpdateNoteRequestDto updateNoteRequestDto = UpdateNoteRequestDto.builder()
                    .title(null)
                    .content(null)
                    .expirationType(ExpirationType.BURN_BY_PERIOD)
                    .expirationPeriod(expirationPeriod)
                    .isAvailable(null)
                    .build();

            NoteDto expNoteAfterUpdate = noteMapper.toDto(noteRepository.findByUrl(url).get());
            expNoteAfterUpdate.setExpirationType(ExpirationType.BURN_BY_PERIOD);
            expNoteAfterUpdate.setExpirationPeriod(expirationPeriod);
            expNoteAfterUpdate.setExpirationFrom(LocalDateTime.now());


            long countOfNotesInReposBeforeUpdate = noteRepository.count();
            NoteDto actualNoteAfterUpdate = noteService.updateNote(url, updateNoteRequestDto);
            long countOfNotesInReposAfterUpdate = noteRepository.count();


            assertNoteDtoEquals(expNoteAfterUpdate, actualNoteAfterUpdate);
            NoteDto actualNoteInRepos = noteMapper.toDto(noteRepository.findByUrl(url).get());
            NoteDto actualNoteInCache = noteMapper.toDto(noteCache.get(url).get());
            assertNoteDtoEquals(expNoteAfterUpdate, actualNoteInRepos);
            assertNoteDtoEquals(expNoteAfterUpdate, actualNoteInCache);
            assertEquals(countOfNotesInReposBeforeUpdate, countOfNotesInReposAfterUpdate);
        }

        @Test
        void expirationPeriodNotNullWhenExpTypeNull(){

            NoteDto noteInReposBeforeUpdate = generateNoteToReposWithExpTypeBurnByPeriod();

            Duration expirationPeriod = Duration.ofMinutes(37);
            String url = noteInReposBeforeUpdate.getUrl();
            UpdateNoteRequestDto updateNoteRequestDto = UpdateNoteRequestDto.builder()
                    .title(null)
                    .content(null)
                    .expirationType(null)
                    .expirationPeriod(expirationPeriod)
                    .isAvailable(null)
                    .build();


            long countOfNotesInReposBeforeUpdate = noteRepository.count();
            GroupValidationException groupOfExceptions = assertThrows(
                    GroupValidationException.class,
                    () -> {
                        noteService.updateNote(url, updateNoteRequestDto);
                    }
            );
            long countOfNotesInReposAfterUpdate = noteRepository.count();


            List<ClientExceptionName> expected = List.of(
                    ClientExceptionName.INVALID_EXPIRATION_PERIOD
            );
            List<ClientExceptionName> actual = groupOfExceptions.getExceptions().stream()
                    .map(ValidationException::getExceptionName)
                    .sorted(Comparator.comparing(Enum::ordinal))
                    .toList();
            assertEquals(expected.size(), actual.size());
            assertEquals(expected, actual);

            NoteDto actualNoteInRepos = noteMapper.toDto(noteRepository.findByUrl(url).get());
            NoteDto actualNoteInCache = noteMapper.toDto(noteCache.get(url).get());
            assertNoteDtoEquals(noteInReposBeforeUpdate, actualNoteInRepos);
            assertNoteDtoEquals(noteInReposBeforeUpdate, actualNoteInCache);

            assertEquals(countOfNotesInReposBeforeUpdate, countOfNotesInReposAfterUpdate);
        }

        @Test
        void changeExpTypeFromNeverToBurnAfterRead(){

            NoteDto noteInReposBeforeUpdate = generateNoteToRepos();

            String url = noteInReposBeforeUpdate.getUrl();
            UpdateNoteRequestDto updateNoteRequestDto = UpdateNoteRequestDto.builder()
                    .title(null)
                    .content(null)
                    .expirationType(ExpirationType.BURN_AFTER_READ)
                    .expirationPeriod(null)
                    .isAvailable(null)
                    .build();

            NoteDto expNoteAfterUpdate = noteMapper.toDto(noteRepository.findByUrl(url).get());
            expNoteAfterUpdate.setExpirationType(ExpirationType.BURN_AFTER_READ);


            long countOfNotesInReposBeforeUpdate = noteRepository.count();
            NoteDto actualNoteAfterUpdate = noteService.updateNote(url, updateNoteRequestDto);
            long countOfNotesInReposAfterUpdate = noteRepository.count();


            assertNoteDtoEquals(expNoteAfterUpdate, actualNoteAfterUpdate);
            NoteDto actualNoteInRepos = noteMapper.toDto(noteRepository.findByUrl(url).get());
            NoteDto actualNoteInCache = noteMapper.toDto(noteCache.get(url).get());
            assertNoteDtoEquals(expNoteAfterUpdate, actualNoteInRepos);
            assertNoteDtoEquals(expNoteAfterUpdate, actualNoteInCache);
            assertEquals(countOfNotesInReposBeforeUpdate, countOfNotesInReposAfterUpdate);
        }

        @Test
        void changeExpTypeFromNeverToBurnByPeriod(){

            NoteDto noteInReposBeforeUpdate = generateNoteToRepos();

            String url = noteInReposBeforeUpdate.getUrl();
            Duration expirationPeriod = Duration.ofMinutes(37);
            UpdateNoteRequestDto updateNoteRequestDto = UpdateNoteRequestDto.builder()
                    .title(null)
                    .content(null)
                    .expirationType(ExpirationType.BURN_BY_PERIOD)
                    .expirationPeriod(expirationPeriod)
                    .isAvailable(null)
                    .build();

            NoteDto expNoteAfterUpdate = noteMapper.toDto(noteRepository.findByUrl(url).get());
            expNoteAfterUpdate.setExpirationType(ExpirationType.BURN_BY_PERIOD);
            expNoteAfterUpdate.setExpirationPeriod(expirationPeriod);


            long countOfNotesInReposBeforeUpdate = noteRepository.count();
            expNoteAfterUpdate.setExpirationFrom(LocalDateTime.now());
            NoteDto actualNoteAfterUpdate = noteService.updateNote(url, updateNoteRequestDto);
            long countOfNotesInReposAfterUpdate = noteRepository.count();


            assertNoteDtoEquals(expNoteAfterUpdate, actualNoteAfterUpdate);
            NoteDto actualNoteInRepos = noteMapper.toDto(noteRepository.findByUrl(url).get());
            NoteDto actualNoteInCache = noteMapper.toDto(noteCache.get(url).get());
            assertNoteDtoEquals(expNoteAfterUpdate, actualNoteInRepos);
            assertNoteDtoEquals(expNoteAfterUpdate, actualNoteInCache);
            assertEquals(countOfNotesInReposBeforeUpdate, countOfNotesInReposAfterUpdate);
        }

        @Test
        void changeExpTypeFromBurnAfterReadToNever(){

            NoteDto noteInReposBeforeUpdate = generateNoteToReposWithExpTypeBurnAfterRead();

            String url = noteInReposBeforeUpdate.getUrl();
            UpdateNoteRequestDto updateNoteRequestDto = UpdateNoteRequestDto.builder()
                    .title(null)
                    .content(null)
                    .expirationType(ExpirationType.BURN_AFTER_READ)
                    .expirationPeriod(null)
                    .isAvailable(null)
                    .build();

            NoteDto expNoteAfterUpdate = noteMapper.toDto(noteRepository.findByUrl(url).get());
            expNoteAfterUpdate.setExpirationType(ExpirationType.BURN_AFTER_READ);


            long countOfNotesInReposBeforeUpdate = noteRepository.count();
            NoteDto actualNoteAfterUpdate = noteService.updateNote(url, updateNoteRequestDto);
            long countOfNotesInReposAfterUpdate = noteRepository.count();


            assertNoteDtoEquals(expNoteAfterUpdate, actualNoteAfterUpdate);
            NoteDto actualNoteInRepos = noteMapper.toDto(noteRepository.findByUrl(url).get());
            NoteDto actualNoteInCache = noteMapper.toDto(noteCache.get(url).get());
            assertNoteDtoEquals(expNoteAfterUpdate, actualNoteInRepos);
            assertNoteDtoEquals(expNoteAfterUpdate, actualNoteInCache);
            assertEquals(countOfNotesInReposBeforeUpdate, countOfNotesInReposAfterUpdate);
        }

        @Test
        void changeExpTypeFromBurnAfterReadToBurnByPeriod(){

            NoteDto noteInReposBeforeUpdate = generateNoteToReposWithExpTypeBurnAfterRead();

            String url = noteInReposBeforeUpdate.getUrl();
            Duration expirationPeriod = Duration.ofMinutes(37);
            UpdateNoteRequestDto updateNoteRequestDto = UpdateNoteRequestDto.builder()
                    .title(null)
                    .content(null)
                    .expirationType(ExpirationType.BURN_BY_PERIOD)
                    .expirationPeriod(expirationPeriod)
                    .isAvailable(null)
                    .build();

            NoteDto expNoteAfterUpdate = noteMapper.toDto(noteRepository.findByUrl(url).get());
            expNoteAfterUpdate.setExpirationType(ExpirationType.BURN_BY_PERIOD);
            expNoteAfterUpdate.setExpirationPeriod(expirationPeriod);


            long countOfNotesInReposBeforeUpdate = noteRepository.count();
            expNoteAfterUpdate.setExpirationFrom(LocalDateTime.now());
            NoteDto actualNoteAfterUpdate = noteService.updateNote(url, updateNoteRequestDto);
            long countOfNotesInReposAfterUpdate = noteRepository.count();


            assertNoteDtoEquals(expNoteAfterUpdate, actualNoteAfterUpdate);
            NoteDto actualNoteInRepos = noteMapper.toDto(noteRepository.findByUrl(url).get());
            NoteDto actualNoteInCache = noteMapper.toDto(noteCache.get(url).get());
            assertNoteDtoEquals(expNoteAfterUpdate, actualNoteInRepos);
            assertNoteDtoEquals(expNoteAfterUpdate, actualNoteInCache);
            assertEquals(countOfNotesInReposBeforeUpdate, countOfNotesInReposAfterUpdate);
        }

        @Test
        void changeExpTypeFromBurnByPeriodToNever(){

            NoteDto noteInReposBeforeUpdate = generateNoteToReposWithExpTypeBurnByPeriod();

            String url = noteInReposBeforeUpdate.getUrl();
            UpdateNoteRequestDto updateNoteRequestDto = UpdateNoteRequestDto.builder()
                    .title(null)
                    .content(null)
                    .expirationType(ExpirationType.NEVER)
                    .expirationPeriod(null)
                    .isAvailable(null)
                    .build();

            NoteDto expNoteAfterUpdate = noteMapper.toDto(noteRepository.findByUrl(url).get());
            expNoteAfterUpdate.setExpirationType(ExpirationType.NEVER);
            expNoteAfterUpdate.setExpirationPeriod(null);
            expNoteAfterUpdate.setExpirationFrom(null);


            long countOfNotesInReposBeforeUpdate = noteRepository.count();
            NoteDto actualNoteAfterUpdate = noteService.updateNote(url, updateNoteRequestDto);
            long countOfNotesInReposAfterUpdate = noteRepository.count();


            assertNoteDtoEquals(expNoteAfterUpdate, actualNoteAfterUpdate);
            NoteDto actualNoteInRepos = noteMapper.toDto(noteRepository.findByUrl(url).get());
            NoteDto actualNoteInCache = noteMapper.toDto(noteCache.get(url).get());
            assertNoteDtoEquals(expNoteAfterUpdate, actualNoteInRepos);
            assertNoteDtoEquals(expNoteAfterUpdate, actualNoteInCache);
            assertEquals(countOfNotesInReposBeforeUpdate, countOfNotesInReposAfterUpdate);
        }

        @Test
        void changeExpTypeFromBurnByPeriodToBurnAfterRead(){

            NoteDto noteInReposBeforeUpdate = generateNoteToReposWithExpTypeBurnByPeriod();

            String url = noteInReposBeforeUpdate.getUrl();
            UpdateNoteRequestDto updateNoteRequestDto = UpdateNoteRequestDto.builder()
                    .title(null)
                    .content(null)
                    .expirationType(ExpirationType.BURN_AFTER_READ)
                    .expirationPeriod(null)
                    .isAvailable(null)
                    .build();

            NoteDto expNoteAfterUpdate = noteMapper.toDto(noteRepository.findByUrl(url).get());
            expNoteAfterUpdate.setExpirationType(ExpirationType.BURN_AFTER_READ);
            expNoteAfterUpdate.setExpirationPeriod(null);
            expNoteAfterUpdate.setExpirationFrom(null);


            long countOfNotesInReposBeforeUpdate = noteRepository.count();
            NoteDto actualNoteAfterUpdate = noteService.updateNote(url, updateNoteRequestDto);
            long countOfNotesInReposAfterUpdate = noteRepository.count();


            assertNoteDtoEquals(expNoteAfterUpdate, actualNoteAfterUpdate);
            NoteDto actualNoteInRepos = noteMapper.toDto(noteRepository.findByUrl(url).get());
            NoteDto actualNoteInCache = noteMapper.toDto(noteCache.get(url).get());
            assertNoteDtoEquals(expNoteAfterUpdate, actualNoteInRepos);
            assertNoteDtoEquals(expNoteAfterUpdate, actualNoteInCache);
            assertEquals(countOfNotesInReposBeforeUpdate, countOfNotesInReposAfterUpdate);
        }

        @Test
        void updateWithNoSeeingDifference(){

            NoteDto noteInReposBeforeUpdate = generateNoteToRepos();

            String title = "New note";
            String content = "My content";
            String url = noteInReposBeforeUpdate.getUrl();
            UpdateNoteRequestDto updateNoteRequestDto = UpdateNoteRequestDto.builder()
                    .title(title)
                    .content(content)
                    .expirationType(null)
                    .expirationPeriod(null)
                    .isAvailable(null)
                    .build();

            NoteDto expNoteAfterUpdate = noteMapper.toDto(noteRepository.findByUrl(url).get());
            expNoteAfterUpdate.setTitle(title);
            expNoteAfterUpdate.setContent(content);


            long countOfNotesInReposBeforeUpdate = noteRepository.count();
            NoteDto actualNoteAfterUpdate = noteService.updateNote(url, updateNoteRequestDto);
            long countOfNotesInReposAfterUpdate = noteRepository.count();


            assertNoteDtoEquals(expNoteAfterUpdate, actualNoteAfterUpdate);
            NoteDto actualNoteInRepos = noteMapper.toDto(noteRepository.findByUrl(url).get());
            NoteDto actualNoteInCache = noteMapper.toDto(noteCache.get(url).get());
            assertNoteDtoEquals(expNoteAfterUpdate, actualNoteInRepos);
            assertNoteDtoEquals(expNoteAfterUpdate, actualNoteInCache);
            assertEquals(countOfNotesInReposBeforeUpdate, countOfNotesInReposAfterUpdate);
        }

        @Test
        void changesAllFields(){

            NoteDto noteInReposBeforeUpdate = generateNoteToRepos();

            String url = noteInReposBeforeUpdate.getUrl();
            String title = "Updated note";
            String content = "My updated content";
            ExpirationType expirationType = ExpirationType.BURN_BY_PERIOD;
            Duration expirationPeriod = Duration.ofMinutes(37);
            Boolean isAvailable = Boolean.FALSE;
            UpdateNoteRequestDto updateNoteRequestDto = UpdateNoteRequestDto.builder()
                    .title(title)
                    .content(content)
                    .expirationType(expirationType)
                    .expirationPeriod(expirationPeriod)
                    .isAvailable(isAvailable)
                    .build();

            NoteDto expNoteAfterUpdate = noteMapper.toDto(noteRepository.findByUrl(url).get());
            expNoteAfterUpdate.setTitle(title);
            expNoteAfterUpdate.setContent(content);
            expNoteAfterUpdate.setExpirationType(expirationType);
            expNoteAfterUpdate.setExpirationPeriod(expirationPeriod);
            expNoteAfterUpdate.setAvailable(isAvailable);


            long countOfNotesInReposBeforeUpdate = noteRepository.count();
            expNoteAfterUpdate.setExpirationFrom(LocalDateTime.now());
            NoteDto actualNoteAfterUpdate = noteService.updateNote(url, updateNoteRequestDto);
            long countOfNotesInReposAfterUpdate = noteRepository.count();


            assertNoteDtoEquals(expNoteAfterUpdate, actualNoteAfterUpdate);
            NoteDto actualNoteInRepos = noteMapper.toDto(noteRepository.findByUrl(url).get());
            NoteDto actualNoteInCache = noteMapper.toDto(noteCache.get(url).get());
            assertNoteDtoEquals(expNoteAfterUpdate, actualNoteInRepos);
            assertNoteDtoEquals(expNoteAfterUpdate, actualNoteInCache);
            assertEquals(countOfNotesInReposBeforeUpdate, countOfNotesInReposAfterUpdate);
        }

        @Test
        void updateTwoNotes() {

            List<NoteDto> noteInReposBeforeUpdate = generateNotesToRepos(2);

            String url1 = noteInReposBeforeUpdate.get(0).getUrl();
            String title1 = "Updated note";
            String content1 = "My updated content";
            ExpirationType expirationType1 = ExpirationType.BURN_BY_PERIOD;
            Duration expirationPeriod1 = Duration.ofMinutes(37);
            Boolean isAvailable1 = Boolean.FALSE;
            UpdateNoteRequestDto updateNoteRequestDto1 = UpdateNoteRequestDto.builder()
                    .title(title1)
                    .content(content1)
                    .expirationType(expirationType1)
                    .expirationPeriod(expirationPeriod1)
                    .isAvailable(isAvailable1)
                    .build();

            NoteDto expNoteAfterUpdate1 = noteMapper.toDto(noteRepository.findByUrl(url1).get());
            expNoteAfterUpdate1.setTitle(title1);
            expNoteAfterUpdate1.setContent(content1);
            expNoteAfterUpdate1.setExpirationType(expirationType1);
            expNoteAfterUpdate1.setExpirationPeriod(expirationPeriod1);
            expNoteAfterUpdate1.setAvailable(isAvailable1);

            String url2 = noteInReposBeforeUpdate.get(1).getUrl();
            String title2 = "Updated second note";
            String content2 = "My second updated content";
            ExpirationType expirationType2 = ExpirationType.BURN_BY_PERIOD;
            Duration expirationPeriod2 = Duration.ofMinutes(39);
            Boolean isAvailable2 = Boolean.FALSE;
            UpdateNoteRequestDto updateNoteRequestDto2 = UpdateNoteRequestDto.builder()
                    .title(title2)
                    .content(content2)
                    .expirationType(expirationType2)
                    .expirationPeriod(expirationPeriod2)
                    .isAvailable(isAvailable2)
                    .build();

            NoteDto expNoteAfterUpdate2 = noteMapper.toDto(noteRepository.findByUrl(url2).get());
            expNoteAfterUpdate2.setTitle(title2);
            expNoteAfterUpdate2.setContent(content2);
            expNoteAfterUpdate2.setExpirationType(expirationType2);
            expNoteAfterUpdate2.setExpirationPeriod(expirationPeriod2);
            expNoteAfterUpdate2.setAvailable(isAvailable2);

            // ----------------------------------------------------------------------------

            long countOfNotesInReposBeforeUpdate = noteRepository.count();
            expNoteAfterUpdate1.setExpirationFrom(LocalDateTime.now());
            NoteDto actualNoteAfterUpdate1 = noteService.updateNote(url1, updateNoteRequestDto1);
            expNoteAfterUpdate2.setExpirationFrom(LocalDateTime.now());
            NoteDto actualNoteAfterUpdate2 = noteService.updateNote(url2, updateNoteRequestDto2);
            long countOfNotesInReposAfterUpdate = noteRepository.count();

            // ----------------------------------------------------------------------------

            assertEquals(countOfNotesInReposBeforeUpdate, countOfNotesInReposAfterUpdate);

            assertNoteDtoEquals(expNoteAfterUpdate1, actualNoteAfterUpdate1);
            NoteDto actualFirstNoteInRepos = noteMapper.toDto(noteRepository.findByUrl(url1).get());
            NoteDto actualFirstNoteInCache = noteMapper.toDto(noteCache.get(url1).get());
            assertNoteDtoEquals(expNoteAfterUpdate1, actualFirstNoteInRepos);
            assertNoteDtoEquals(expNoteAfterUpdate1, actualFirstNoteInCache);

            assertNoteDtoEquals(expNoteAfterUpdate2, actualNoteAfterUpdate2);
            NoteDto actualSecondNoteInRepos = noteMapper.toDto(noteRepository.findByUrl(url2).get());
            NoteDto actualSecondNoteInCache = noteMapper.toDto(noteCache.get(url2).get());
            assertNoteDtoEquals(expNoteAfterUpdate2, actualSecondNoteInRepos);
            assertNoteDtoEquals(expNoteAfterUpdate2, actualSecondNoteInCache);
        }

        @Test
        void updateNoteTwoTimes() {

            NoteDto noteInReposBeforeUpdate = generateNoteToRepos();

            String url = noteInReposBeforeUpdate.getUrl();
            String title1 = "Updated note";
            String content1 = "My updated content";
            ExpirationType expirationType1 = ExpirationType.BURN_AFTER_READ;
            Duration expirationPeriod1 = null;
            Boolean isAvailable1 = Boolean.FALSE;
            UpdateNoteRequestDto updateNoteRequestDto1 = UpdateNoteRequestDto.builder()
                    .title(title1)
                    .content(content1)
                    .expirationType(expirationType1)
                    .expirationPeriod(expirationPeriod1)
                    .isAvailable(isAvailable1)
                    .build();

            String title2 = "Updated note second time";
            String content2 = "My second time updated content";
            ExpirationType expirationType2 = ExpirationType.BURN_BY_PERIOD;
            Duration expirationPeriod2 = Duration.ofMinutes(39);
            Boolean isAvailable2 = Boolean.FALSE;
            UpdateNoteRequestDto updateNoteRequestDto2 = UpdateNoteRequestDto.builder()
                    .title(title2)
                    .content(content2)
                    .expirationType(expirationType2)
                    .expirationPeriod(expirationPeriod2)
                    .isAvailable(isAvailable2)
                    .build();

            NoteDto expNoteAfterUpdate = noteMapper.toDto(noteRepository.findByUrl(url).get());
            expNoteAfterUpdate.setTitle(title2);
            expNoteAfterUpdate.setContent(content2);
            expNoteAfterUpdate.setExpirationType(expirationType2);
            expNoteAfterUpdate.setExpirationPeriod(expirationPeriod2);
            expNoteAfterUpdate.setExpirationFrom(null);
            expNoteAfterUpdate.setAvailable(isAvailable2);

            // ----------------------------------------------------------------------------

            long countOfNotesInReposBeforeUpdate = noteRepository.count();
            noteService.updateNote(url, updateNoteRequestDto1);

            expNoteAfterUpdate.setExpirationFrom(LocalDateTime.now());
            NoteDto actualNoteAfterUpdate = noteService.updateNote(url, updateNoteRequestDto2);
            long countOfNotesInReposAfterUpdate = noteRepository.count();

            // ----------------------------------------------------------------------------

            assertNoteDtoEquals(expNoteAfterUpdate, actualNoteAfterUpdate);
            NoteDto actualNoteInRepos = noteMapper.toDto(noteRepository.findByUrl(url).get());
            NoteDto actualNoteInCache = noteMapper.toDto(noteCache.get(url).get());
            assertNoteDtoEquals(expNoteAfterUpdate, actualNoteInRepos);
            assertNoteDtoEquals(expNoteAfterUpdate, actualNoteInCache);
            assertEquals(countOfNotesInReposBeforeUpdate, countOfNotesInReposAfterUpdate);
        }

        @Test
        public void concurrentUpdateOfNote() {

            NoteDto noteInReposBeforeUpdate = generateNoteToRepos();

            String url = noteInReposBeforeUpdate.getUrl();
            String title1 = "Updated note";
            String content1 = "My updated content";
            ExpirationType expirationType1 = ExpirationType.NEVER;
            Duration expirationPeriod1 = null;
            Boolean isAvailable1 = Boolean.FALSE;
            UpdateNoteRequestDto updateNoteRequestDto1 = UpdateNoteRequestDto.builder()
                    .title(title1)
                    .content(content1)
                    .expirationType(expirationType1)
                    .expirationPeriod(expirationPeriod1)
                    .isAvailable(isAvailable1)
                    .build();

            String title2 = "Updated note second time";
            String content2 = "My second time updated content";
            ExpirationType expirationType2 = ExpirationType.NEVER;
            Duration expirationPeriod2 = null;
            Boolean isAvailable2 = Boolean.FALSE;
            UpdateNoteRequestDto updateNoteRequestDto2 = UpdateNoteRequestDto.builder()
                    .title(title2)
                    .content(content2)
                    .expirationType(expirationType2)
                    .expirationPeriod(expirationPeriod2)
                    .isAvailable(isAvailable2)
                    .build();

            NoteDto expNoteAfterUpdate1 = noteMapper.toDto(noteRepository.findByUrl(url).get());
            expNoteAfterUpdate1.setTitle(title1);
            expNoteAfterUpdate1.setContent(content1);
            expNoteAfterUpdate1.setExpirationType(expirationType1);
            expNoteAfterUpdate1.setExpirationPeriod(expirationPeriod1);
            expNoteAfterUpdate1.setAvailable(isAvailable1);

            NoteDto expNoteAfterUpdate2 = noteMapper.toDto(noteRepository.findByUrl(url).get());
            expNoteAfterUpdate2.setTitle(title2);
            expNoteAfterUpdate2.setContent(content2);
            expNoteAfterUpdate2.setExpirationType(expirationType2);
            expNoteAfterUpdate2.setExpirationPeriod(expirationPeriod2);
            expNoteAfterUpdate2.setAvailable(isAvailable2);


            CompletableFuture<NoteDto> futureNoteFirst = CompletableFuture.supplyAsync(
                    () -> noteService.updateNote(url, updateNoteRequestDto1), executors);

            CompletableFuture<NoteDto> futureNoteSecond = CompletableFuture.supplyAsync(
                    () -> noteService.updateNote(url, updateNoteRequestDto2), executors);

            CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(futureNoteFirst, futureNoteSecond);
            long countOfNotesInReposBeforeUpdate = noteRepository.count();
            combinedFuture.join();
            long countOfNotesInReposAfterUpdate = noteRepository.count();


            assertEquals(countOfNotesInReposBeforeUpdate, countOfNotesInReposAfterUpdate);
            NoteDto actualNoteInRepos = noteMapper.toDto(noteRepository.findByUrl(url).get());
            NoteDto actualNoteInCache = noteMapper.toDto(noteCache.get(url).get());
            NoteDto updatedNoteInRep = noteMapper.toDto(noteRepository.findByUrl(url).get());

            boolean firstPassed = false;
            try {
                assertNoteDtoEquals(expNoteAfterUpdate1, updatedNoteInRep);
                assertNoteDtoEquals(expNoteAfterUpdate1, actualNoteInRepos);
                assertNoteDtoEquals(expNoteAfterUpdate1, actualNoteInCache);
                firstPassed = true;
            } catch (AssertionError ignored) {
            }

            if (!firstPassed) {
                assertNoteDtoEquals(expNoteAfterUpdate2, updatedNoteInRep);
                assertNoteDtoEquals(expNoteAfterUpdate2, actualNoteInRepos);
                assertNoteDtoEquals(expNoteAfterUpdate2, actualNoteInCache);
            }
        }

        // validation ------------------------------------------------------------------------------------------------------

        @Test
        void allFieldsAreNull(){

            NoteDto noteInReposBeforeUpdate = generateNoteToRepos();

            String url = noteInReposBeforeUpdate.getUrl();
            UpdateNoteRequestDto updateNoteRequestDto = UpdateNoteRequestDto.builder()
                    .title(null)
                    .content(null)
                    .expirationType(null)
                    .expirationPeriod(null)
                    .isAvailable(null)
                    .build();


            long countOfNotesInReposBeforeUpdate = noteRepository.count();
            GroupValidationException groupOfExceptions = assertThrows(
                    GroupValidationException.class,
                    () -> {
                        noteService.updateNote(url, updateNoteRequestDto);
                    }
            );
            long countOfNotesInReposAfterUpdate = noteRepository.count();


            List<ClientExceptionName> expected = List.of(
                    ClientExceptionName.EMPTY_UPDATE_REQUEST
            );
            List<ClientExceptionName> actual = groupOfExceptions.getExceptions().stream()
                    .map(ValidationException::getExceptionName)
                    .sorted(Comparator.comparing(Enum::ordinal))
                    .toList();
            assertEquals(expected.size(), actual.size());
            assertEquals(expected, actual);

            assertNoteDtoEquals(noteInReposBeforeUpdate, noteMapper.toDto(noteRepository.findByUrl(url).get()));
            assertNoteDtoEquals(noteInReposBeforeUpdate, noteMapper.toDto(noteCache.get(url).get()));

            assertEquals(countOfNotesInReposBeforeUpdate, countOfNotesInReposAfterUpdate);
        }

        @Test
        void invalidTitle(){

            NoteDto noteInReposBeforeUpdate = generateNoteToRepos();

            String url = noteInReposBeforeUpdate.getUrl();
            UpdateNoteRequestDto updateNoteRequestDto = UpdateNoteRequestDto.builder()
                    .title(".Hello")
                    .content(null)
                    .expirationType(null)
                    .expirationPeriod(null)
                    .isAvailable(null)
                    .build();


            long countOfNotesInReposBeforeUpdate = noteRepository.count();
            GroupValidationException groupOfExceptions = assertThrows(
                    GroupValidationException.class,
                    () -> {
                        noteService.updateNote(url, updateNoteRequestDto);
                    }
            );
            long countOfNotesInReposAfterUpdate = noteRepository.count();


            List<ClientExceptionName> expected = List.of(
                    ClientExceptionName.INVALID_TITLE
            );
            List<ClientExceptionName> actual = groupOfExceptions.getExceptions().stream()
                    .map(ValidationException::getExceptionName)
                    .sorted(Comparator.comparing(Enum::ordinal))
                    .toList();
            assertEquals(expected.size(), actual.size());
            assertEquals(expected, actual);

            assertNoteDtoEquals(noteInReposBeforeUpdate, noteMapper.toDto(noteRepository.findByUrl(url).get()));
            assertNoteDtoEquals(noteInReposBeforeUpdate, noteMapper.toDto(noteCache.get(url).get()));

            assertEquals(countOfNotesInReposBeforeUpdate, countOfNotesInReposAfterUpdate);
        }

        @Test
        void invalidContent(){

            NoteDto noteInReposBeforeUpdate = generateNoteToRepos();

            String url = noteInReposBeforeUpdate.getUrl();
            UpdateNoteRequestDto updateNoteRequestDto = UpdateNoteRequestDto.builder()
                    .title(null)
                    .content(stringOfLength(contentLength + 1))
                    .expirationType(null)
                    .expirationPeriod(null)
                    .isAvailable(null)
                    .build();


            long countOfNotesInReposBeforeUpdate = noteRepository.count();
            GroupValidationException groupOfExceptions = assertThrows(
                    GroupValidationException.class,
                    () -> {
                        noteService.updateNote(url, updateNoteRequestDto);
                    }
            );
            long countOfNotesInReposAfterUpdate = noteRepository.count();


            List<ClientExceptionName> expected = List.of(
                    ClientExceptionName.INVALID_CONTENT
            );
            List<ClientExceptionName> actual = groupOfExceptions.getExceptions().stream()
                    .map(ValidationException::getExceptionName)
                    .sorted(Comparator.comparing(Enum::ordinal))
                    .toList();
            assertEquals(expected.size(), actual.size());
            assertEquals(expected, actual);

            assertNoteDtoEquals(noteInReposBeforeUpdate, noteMapper.toDto(noteRepository.findByUrl(url).get()));
            assertNoteDtoEquals(noteInReposBeforeUpdate, noteMapper.toDto(noteCache.get(url).get()));

            assertEquals(countOfNotesInReposBeforeUpdate, countOfNotesInReposAfterUpdate);
        }

        @Test
        void invalidExpirationPeriod(){

            NoteDto noteInReposBeforeUpdate = generateNoteToRepos();

            Duration expirationPeriod = null;
            String url = noteInReposBeforeUpdate.getUrl();
            UpdateNoteRequestDto updateNoteRequestDto = UpdateNoteRequestDto.builder()
                    .title(null)
                    .content(null)
                    .expirationType(ExpirationType.BURN_BY_PERIOD)
                    .expirationPeriod(expirationPeriod)
                    .isAvailable(null)
                    .build();


            long countOfNotesInReposBeforeUpdate = noteRepository.count();
            GroupValidationException groupOfExceptions = assertThrows(
                    GroupValidationException.class,
                    () -> {
                        noteService.updateNote(url, updateNoteRequestDto);
                    }
            );
            long countOfNotesInReposAfterUpdate = noteRepository.count();


            List<ClientExceptionName> expected = List.of(
                    ClientExceptionName.INVALID_EXPIRATION_PERIOD
            );
            List<ClientExceptionName> actual = groupOfExceptions.getExceptions().stream()
                    .map(ValidationException::getExceptionName)
                    .sorted(Comparator.comparing(Enum::ordinal))
                    .toList();
            assertEquals(expected.size(), actual.size());
            assertEquals(expected, actual);

            assertNoteDtoEquals(noteInReposBeforeUpdate, noteMapper.toDto(noteRepository.findByUrl(url).get()));
            assertNoteDtoEquals(noteInReposBeforeUpdate, noteMapper.toDto(noteCache.get(url).get()));

            assertEquals(countOfNotesInReposBeforeUpdate, countOfNotesInReposAfterUpdate);
        }

        @Test
        void allFieldsInvalid(){

            NoteDto noteInReposBeforeUpdate = generateNoteToRepos();

            Duration expirationPeriod = null;
            String url = noteInReposBeforeUpdate.getUrl();
            UpdateNoteRequestDto updateNoteRequestDto = UpdateNoteRequestDto.builder()
                    .title(".Hello")
                    .content(stringOfLength(contentLength + 1))
                    .expirationType(ExpirationType.BURN_BY_PERIOD)
                    .expirationPeriod(expirationPeriod)
                    .isAvailable(null)
                    .build();


            long countOfNotesInReposBeforeUpdate = noteRepository.count();
            GroupValidationException groupOfExceptions = assertThrows(
                    GroupValidationException.class,
                    () -> {
                        noteService.updateNote(url, updateNoteRequestDto);
                    }
            );
            long countOfNotesInReposAfterUpdate = noteRepository.count();


            List<ClientExceptionName> expected = List.of(
                    ClientExceptionName.INVALID_TITLE,
                    ClientExceptionName.INVALID_CONTENT,
                    ClientExceptionName.INVALID_EXPIRATION_PERIOD
            );
            List<ClientExceptionName> actual = groupOfExceptions.getExceptions().stream()
                    .map(ValidationException::getExceptionName)
                    .sorted(Comparator.comparing(Enum::ordinal))
                    .toList();
            assertEquals(expected.size(), actual.size());
            assertEquals(expected, actual);

            assertNoteDtoEquals(noteInReposBeforeUpdate, noteMapper.toDto(noteRepository.findByUrl(url).get()));
            assertNoteDtoEquals(noteInReposBeforeUpdate, noteMapper.toDto(noteCache.get(url).get()));

            assertEquals(countOfNotesInReposBeforeUpdate, countOfNotesInReposAfterUpdate);
        }
    }

    @Nested
    @DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
    class GetNoteTest {

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

    @Nested
    @DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
    class DeleteNoteTest {

        @Test
        void deleteNote() {

            NoteDto noteBeforeDelete = generateNoteToRepos();
            NoteDto expectedDeletedNote = noteMapper.toDto(noteRepository.findByUrl(noteBeforeDelete.getUrl()).get());
            expectedDeletedNote.setAvailable(false);

            Boolean wasDeleted = noteService.deleteNote(noteBeforeDelete.getUrl());

            assertTrue(wasDeleted);
            NoteDto actualNoteInRepos = noteMapper.toDto(noteRepository.findByUrl(noteBeforeDelete.getUrl()).get());
            NoteDto actualNoteInCache = noteMapper.toDto(noteCache.get(noteBeforeDelete.getUrl()).get());
            assertNoteDtoEquals(expectedDeletedNote, actualNoteInRepos);
            assertNoteDtoEquals(expectedDeletedNote, actualNoteInCache);
        }
    }
}
