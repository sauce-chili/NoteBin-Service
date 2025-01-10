package vstu.isd.notebin.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
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
import vstu.isd.notebin.exception.ClientExceptionName;
import vstu.isd.notebin.exception.GroupValidationException;
import vstu.isd.notebin.exception.ValidationException;
import vstu.isd.notebin.mapper.NoteMapper;
import vstu.isd.notebin.repository.NoteRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static vstu.isd.notebin.testutils.TestAsserts.*;

@SpringBootTest
@ContextConfiguration(initializers = TestContainersConfig.class)
@Slf4j
public class NoteServiceUpdateNoteTest {

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
    String stringOfLength(int length){

        return "a".repeat(Math.max(0, length));
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
        assertNoteDtoEquals(expNoteAfterUpdate, noteMapper.toDto(noteRepository.findByUrl(url).get()));
        assertNoteDtoEquals(expNoteAfterUpdate, noteMapper.toDto(noteCache.get(url).get()));
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

        assertNoteDtoEquals(noteInReposBeforeUpdate, noteMapper.toDto(noteRepository.findByUrl(url).get()));
        assertNoteDtoEquals(noteInReposBeforeUpdate, noteMapper.toDto(noteCache.get(url).get()));

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
        assertNoteDtoEquals(expNoteAfterUpdate, noteMapper.toDto(noteRepository.findByUrl(url).get()));
        assertNoteDtoEquals(expNoteAfterUpdate, noteMapper.toDto(noteCache.get(url).get()));
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
        assertNoteDtoEquals(expNoteAfterUpdate, noteMapper.toDto(noteRepository.findByUrl(url).get()));
        assertNoteDtoEquals(expNoteAfterUpdate, noteMapper.toDto(noteCache.get(url).get()));
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
        assertNoteDtoEquals(expNoteAfterUpdate, noteMapper.toDto(noteRepository.findByUrl(url).get()));
        assertNoteDtoEquals(expNoteAfterUpdate, noteMapper.toDto(noteCache.get(url).get()));
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
        assertNoteDtoEquals(expNoteAfterUpdate, noteMapper.toDto(noteRepository.findByUrl(url).get()));
        assertNoteDtoEquals(expNoteAfterUpdate, noteMapper.toDto(noteCache.get(url).get()));
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
        assertNoteDtoEquals(expNoteAfterUpdate, noteMapper.toDto(noteRepository.findByUrl(url).get()));
        assertNoteDtoEquals(expNoteAfterUpdate, noteMapper.toDto(noteCache.get(url).get()));
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
        assertNoteDtoEquals(expNoteAfterUpdate, noteMapper.toDto(noteRepository.findByUrl(url).get()));
        assertNoteDtoEquals(expNoteAfterUpdate, noteMapper.toDto(noteCache.get(url).get()));
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
        assertNoteDtoEquals(expNoteAfterUpdate, noteMapper.toDto(noteRepository.findByUrl(url).get()));
        assertNoteDtoEquals(expNoteAfterUpdate, noteMapper.toDto(noteCache.get(url).get()));
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
        assertNoteDtoEquals(expNoteAfterUpdate, noteMapper.toDto(noteRepository.findByUrl(url).get()));
        assertNoteDtoEquals(expNoteAfterUpdate, noteMapper.toDto(noteCache.get(url).get()));
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
        assertNoteDtoEquals(expNoteAfterUpdate1, noteMapper.toDto(noteRepository.findByUrl(url1).get()));
        assertNoteDtoEquals(expNoteAfterUpdate1, noteMapper.toDto(noteCache.get(url1).get()));

        assertNoteDtoEquals(expNoteAfterUpdate2, actualNoteAfterUpdate2);
        assertNoteDtoEquals(expNoteAfterUpdate2, noteMapper.toDto(noteRepository.findByUrl(url2).get()));
        assertNoteDtoEquals(expNoteAfterUpdate2, noteMapper.toDto(noteCache.get(url2).get()));
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
        assertNoteDtoEquals(expNoteAfterUpdate, noteMapper.toDto(noteRepository.findByUrl(url).get()));
        assertNoteDtoEquals(expNoteAfterUpdate, noteMapper.toDto(noteCache.get(url).get()));
        assertEquals(countOfNotesInReposBeforeUpdate, countOfNotesInReposAfterUpdate);
    }

    @Test
    public void concurrentUpdateOfNote() {

        NoteDto noteInReposBeforeUpdate = generateNoteToRepos();

        String url = noteInReposBeforeUpdate.getUrl();
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
        expNoteAfterUpdate1.setExpirationFrom(null);
        expNoteAfterUpdate2.setExpirationFrom(null);
        long countOfNotesInReposBeforeUpdate = noteRepository.count();
        combinedFuture.join();
        long countOfNotesInReposAfterUpdate = noteRepository.count();


        assertEquals(countOfNotesInReposBeforeUpdate, countOfNotesInReposAfterUpdate);
        NoteDto updatedNoteInRep = noteMapper.toDto(noteRepository.findByUrl(url).get());
        updatedNoteInRep.setExpirationFrom(null);

        boolean firstPassed = false;
        try {
            assertNoteDtoEquals(expNoteAfterUpdate1, updatedNoteInRep);
            firstPassed = true;
        } catch (AssertionError ignored) {
        }

        if (!firstPassed) {
            assertNoteDtoEquals(expNoteAfterUpdate2, updatedNoteInRep);
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