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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static vstu.isd.notebin.testutils.TestAsserts.*;

@SpringBootTest
@ContextConfiguration(initializers = TestContainersConfig.class)
@Slf4j
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
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

    void addNoteInRepos(){

        String title = "New note";
        String content = "My content";
        CreateNoteRequestDto createNoteRequestDto = CreateNoteRequestDto.builder()
                .title(title)
                .content(content)
                .expirationType(ExpirationType.NEVER)
                .expirationPeriod(null)
                .build();

        noteService.createNote(createNoteRequestDto);
    }

    void addNoteInReposWithExpTypeBurnAfterRead(){

        String title = "New note";
        String content = "My content";
        CreateNoteRequestDto createNoteRequestDto = CreateNoteRequestDto.builder()
                .title(title)
                .content(content)
                .expirationType(ExpirationType.BURN_AFTER_READ)
                .expirationPeriod(null)
                .build();

        noteService.createNote(createNoteRequestDto);
    }

    void addNoteInReposWithExpTypeBurnByPeriod(){

        String title = "New note";
        String content = "My content";
        Duration expirationPeriod = Duration.ofMinutes(15);
        CreateNoteRequestDto createNoteRequestDto = CreateNoteRequestDto.builder()
                .title(title)
                .content(content)
                .expirationType(ExpirationType.BURN_BY_PERIOD)
                .expirationPeriod(expirationPeriod)
                .build();

        noteService.createNote(createNoteRequestDto);
    }

    void addNotesInRepos(int count){

        String defaultTitle = "My title";
        String defaultContent = "My content";
        ExpirationType expirationType = ExpirationType.NEVER;

        for (int i = 0; i < count; i++) {

            LocalDateTime now = LocalDateTime.now();
            CreateNoteRequestDto createNoteRequestDto = CreateNoteRequestDto.builder()
                    .title(defaultTitle + " " + i)
                    .content(defaultContent + " " + i)
                    .expirationType(expirationType)
                    .expirationPeriod(null)
                    .build();

            noteService.createNote(createNoteRequestDto);
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    @Test
    void updateNote(){

        addNoteInRepos();

        Duration expirationPeriod = Duration.ofMinutes(37);
        String url = "1";
        UpdateNoteRequestDto updateNoteRequestDto = UpdateNoteRequestDto.builder()
                .title(null)
                .content(null)
                .expirationType(ExpirationType.BURN_BY_PERIOD)
                .expirationPeriod(expirationPeriod)
                .isAvailable(null)
                .build();
        GetNoteRequestDto getNoteRequestDto = GetNoteRequestDto.builder()
                .url(url)
                .build();
        long countOfNotesInReposBeforeUpdate = noteRepository.count();

        NoteDto expNoteAfterUpdate = noteService.getNote(getNoteRequestDto);
        expNoteAfterUpdate.setExpirationType(ExpirationType.BURN_BY_PERIOD);
        expNoteAfterUpdate.setExpirationPeriod(expirationPeriod);


        expNoteAfterUpdate.setExpirationFrom(LocalDateTime.now());
        NoteDto actualNoteAfterUpdate = noteService.updateNote(url, updateNoteRequestDto);


        assertNoteDtoEquals(expNoteAfterUpdate, actualNoteAfterUpdate);
        assertNoteExistsInRepository(actualNoteAfterUpdate, noteRepository);
        assertEquals(countOfNotesInReposBeforeUpdate, noteRepository.count());
        assertNoteExistsInCache(actualNoteAfterUpdate, noteCache);
    }

    @Test
    void expirationPeriodNotNullWhenExpTypeNull(){

        addNoteInReposWithExpTypeBurnByPeriod();

        Duration expirationPeriod = Duration.ofMinutes(37);
        String url = "1";
        UpdateNoteRequestDto updateNoteRequestDto = UpdateNoteRequestDto.builder()
                .title(null)
                .content(null)
                .expirationType(null)
                .expirationPeriod(expirationPeriod)
                .isAvailable(null)
                .build();
        GetNoteRequestDto getNoteRequestDto = GetNoteRequestDto.builder()
                .url(url)
                .build();

        NoteDto expNoteAfterUpdate = noteService.getNote(getNoteRequestDto);
        expNoteAfterUpdate.setExpirationPeriod(expirationPeriod);
        Optional<Note> optionalNoteBeforeUpdateInRep = noteRepository.findByUrl(url);
        NoteDto noteBeforeUpdateInRep = noteMapper.toDto(optionalNoteBeforeUpdateInRep.get());


        long countOfNotesInReposBeforeUpdate = noteRepository.count();
        GroupValidationException groupOfExceptions = assertThrows(
                GroupValidationException.class,
                () -> {
                    noteService.updateNote(url, updateNoteRequestDto);
                }
        );
        List<? extends ValidationException> exceptions = groupOfExceptions.getExceptions();


        assertEquals(1, exceptions.size());
        assertEquals(ClientExceptionName.INVALID_EXPIRATION_PERIOD, exceptions.get(0).getExceptionName());

        Optional<Note> updatedOptionalNoteInRep = noteRepository.findByUrl(url);
        NoteDto updatedNoteInRep = noteMapper.toDto(updatedOptionalNoteInRep.get());
        assertNoteDtoEquals(noteBeforeUpdateInRep, updatedNoteInRep);

        assertEquals(countOfNotesInReposBeforeUpdate, noteRepository.count());
    }

    @Test
    void changeExpTypeFromNeverToBurnAfterRead(){

        addNoteInRepos();

        String url = "1";
        UpdateNoteRequestDto updateNoteRequestDto = UpdateNoteRequestDto.builder()
                .title(null)
                .content(null)
                .expirationType(ExpirationType.BURN_AFTER_READ)
                .expirationPeriod(null)
                .isAvailable(null)
                .build();
        GetNoteRequestDto getNoteRequestDto = GetNoteRequestDto.builder()
                .url(url)
                .build();

        NoteDto expNoteAfterUpdate = noteService.getNote(getNoteRequestDto);
        expNoteAfterUpdate.setExpirationType(ExpirationType.BURN_AFTER_READ);


        long countOfNotesInReposBeforeUpdate = noteRepository.count();
        NoteDto actualNoteAfterUpdate = noteService.updateNote(url, updateNoteRequestDto);


        assertNoteDtoEquals(expNoteAfterUpdate, actualNoteAfterUpdate);
        assertNoteExistsInRepository(actualNoteAfterUpdate, noteRepository);
        assertEquals(countOfNotesInReposBeforeUpdate, noteRepository.count());
        assertNoteExistsInCache(actualNoteAfterUpdate, noteCache);
    }

    @Test
    void changeExpTypeFromNeverToBurnByPeriod(){

        addNoteInRepos();

        String url = "1";
        Duration expirationPeriod = Duration.ofMinutes(37);
        UpdateNoteRequestDto updateNoteRequestDto = UpdateNoteRequestDto.builder()
                .title(null)
                .content(null)
                .expirationType(ExpirationType.BURN_BY_PERIOD)
                .expirationPeriod(expirationPeriod)
                .isAvailable(null)
                .build();
        GetNoteRequestDto getNoteRequestDto = GetNoteRequestDto.builder()
                .url(url)
                .build();

        NoteDto expNoteAfterUpdate = noteService.getNote(getNoteRequestDto);
        expNoteAfterUpdate.setExpirationType(ExpirationType.BURN_BY_PERIOD);
        expNoteAfterUpdate.setExpirationPeriod(expirationPeriod);


        long countOfNotesInReposBeforeUpdate = noteRepository.count();
        expNoteAfterUpdate.setExpirationFrom(LocalDateTime.now());
        NoteDto actualNoteAfterUpdate = noteService.updateNote(url, updateNoteRequestDto);


        assertNoteDtoEquals(expNoteAfterUpdate, actualNoteAfterUpdate);
        assertNoteExistsInRepository(actualNoteAfterUpdate, noteRepository);
        assertEquals(countOfNotesInReposBeforeUpdate, noteRepository.count());
        assertNoteExistsInCache(actualNoteAfterUpdate, noteCache);
    }

    @Test
    void changeExpTypeFromBurnAfterReadToNever(){

        addNoteInReposWithExpTypeBurnAfterRead();

        String url = "1";
        UpdateNoteRequestDto updateNoteRequestDto = UpdateNoteRequestDto.builder()
                .title(null)
                .content(null)
                .expirationType(ExpirationType.BURN_AFTER_READ)
                .expirationPeriod(null)
                .isAvailable(null)
                .build();
        GetNoteRequestDto getNoteRequestDto = GetNoteRequestDto.builder()
                .url(url)
                .build();

        NoteDto expNoteAfterUpdate = noteService.getNote(getNoteRequestDto);
        expNoteAfterUpdate.setExpirationType(ExpirationType.BURN_AFTER_READ);


        long countOfNotesInReposBeforeUpdate = noteRepository.count();
        NoteDto actualNoteAfterUpdate = noteService.updateNote(url, updateNoteRequestDto);


        assertNoteDtoEquals(expNoteAfterUpdate, actualNoteAfterUpdate);
        assertNoteExistsInRepository(actualNoteAfterUpdate, noteRepository);
        assertEquals(countOfNotesInReposBeforeUpdate, noteRepository.count());
        assertNoteExistsInCache(actualNoteAfterUpdate, noteCache);
    }

    @Test
    void changeExpTypeFromBurnAfterReadToBurnByPeriod(){

        addNoteInReposWithExpTypeBurnAfterRead();

        String url = "1";
        Duration expirationPeriod = Duration.ofMinutes(37);
        UpdateNoteRequestDto updateNoteRequestDto = UpdateNoteRequestDto.builder()
                .title(null)
                .content(null)
                .expirationType(ExpirationType.BURN_BY_PERIOD)
                .expirationPeriod(expirationPeriod)
                .isAvailable(null)
                .build();
        GetNoteRequestDto getNoteRequestDto = GetNoteRequestDto.builder()
                .url(url)
                .build();

        NoteDto expNoteAfterUpdate = noteService.getNote(getNoteRequestDto);
        expNoteAfterUpdate.setExpirationType(ExpirationType.BURN_BY_PERIOD);
        expNoteAfterUpdate.setExpirationPeriod(expirationPeriod);


        long countOfNotesInReposBeforeUpdate = noteRepository.count();
        expNoteAfterUpdate.setExpirationFrom(LocalDateTime.now());
        NoteDto actualNoteAfterUpdate = noteService.updateNote(url, updateNoteRequestDto);


        assertNoteDtoEquals(expNoteAfterUpdate, actualNoteAfterUpdate);
        assertNoteExistsInRepository(actualNoteAfterUpdate, noteRepository);
        assertEquals(countOfNotesInReposBeforeUpdate, noteRepository.count());
        assertNoteExistsInCache(actualNoteAfterUpdate, noteCache);
    }

    @Test
    void changeExpTypeFromBurnByPeriodToNever(){

        addNoteInReposWithExpTypeBurnByPeriod();

        String url = "1";
        UpdateNoteRequestDto updateNoteRequestDto = UpdateNoteRequestDto.builder()
                .title(null)
                .content(null)
                .expirationType(ExpirationType.NEVER)
                .expirationPeriod(null)
                .isAvailable(null)
                .build();
        GetNoteRequestDto getNoteRequestDto = GetNoteRequestDto.builder()
                .url(url)
                .build();

        NoteDto expNoteAfterUpdate = noteService.getNote(getNoteRequestDto);
        expNoteAfterUpdate.setExpirationType(ExpirationType.NEVER);
        expNoteAfterUpdate.setExpirationPeriod(null);
        expNoteAfterUpdate.setExpirationFrom(null);


        long countOfNotesInReposBeforeUpdate = noteRepository.count();
        NoteDto actualNoteAfterUpdate = noteService.updateNote(url, updateNoteRequestDto);


        assertNoteDtoEquals(expNoteAfterUpdate, actualNoteAfterUpdate);
        assertNoteExistsInRepository(actualNoteAfterUpdate, noteRepository);
        assertEquals(countOfNotesInReposBeforeUpdate, noteRepository.count());
        assertNoteExistsInCache(actualNoteAfterUpdate, noteCache);
    }

    @Test
    void changeExpTypeFromBurnByPeriodToBurnAfterRead(){

        addNoteInReposWithExpTypeBurnByPeriod();

        String url = "1";
        UpdateNoteRequestDto updateNoteRequestDto = UpdateNoteRequestDto.builder()
                .title(null)
                .content(null)
                .expirationType(ExpirationType.BURN_AFTER_READ)
                .expirationPeriod(null)
                .isAvailable(null)
                .build();
        GetNoteRequestDto getNoteRequestDto = GetNoteRequestDto.builder()
                .url(url)
                .build();

        NoteDto expNoteAfterUpdate = noteService.getNote(getNoteRequestDto);
        expNoteAfterUpdate.setExpirationType(ExpirationType.BURN_AFTER_READ);
        expNoteAfterUpdate.setExpirationPeriod(null);
        expNoteAfterUpdate.setExpirationFrom(null);


        long countOfNotesInReposBeforeUpdate = noteRepository.count();
        NoteDto actualNoteAfterUpdate = noteService.updateNote(url, updateNoteRequestDto);


        assertNoteDtoEquals(expNoteAfterUpdate, actualNoteAfterUpdate);
        assertNoteExistsInRepository(actualNoteAfterUpdate, noteRepository);
        assertEquals(countOfNotesInReposBeforeUpdate, noteRepository.count());
        assertNoteExistsInCache(actualNoteAfterUpdate, noteCache);
    }

    @Test
    void updateWithNoSeeingDifference(){

        addNoteInRepos();

        String title = "New note";
        String content = "My content";
        String url = "1";
        UpdateNoteRequestDto updateNoteRequestDto = UpdateNoteRequestDto.builder()
                .title(title)
                .content(content)
                .expirationType(null)
                .expirationPeriod(null)
                .isAvailable(null)
                .build();
        GetNoteRequestDto getNoteRequestDto = GetNoteRequestDto.builder()
                .url(url)
                .build();

        NoteDto expNoteAfterUpdate = noteService.getNote(getNoteRequestDto);
        expNoteAfterUpdate.setTitle(title);
        expNoteAfterUpdate.setContent(content);


        long countOfNotesInReposBeforeUpdate = noteRepository.count();
        NoteDto actualNoteAfterUpdate = noteService.updateNote(url, updateNoteRequestDto);


        assertNoteDtoEquals(expNoteAfterUpdate, actualNoteAfterUpdate);
        assertNoteExistsInRepository(actualNoteAfterUpdate, noteRepository);
        assertEquals(countOfNotesInReposBeforeUpdate, noteRepository.count());
        assertNoteExistsInCache(actualNoteAfterUpdate, noteCache);
    }

    @Test
    void changesAllFields(){

        addNoteInRepos();

        String url = "1";

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
        GetNoteRequestDto getNoteRequestDto = GetNoteRequestDto.builder()
                .url(url)
                .build();

        NoteDto expNoteAfterUpdate = noteService.getNote(getNoteRequestDto);
        expNoteAfterUpdate.setTitle(title);
        expNoteAfterUpdate.setContent(content);
        expNoteAfterUpdate.setExpirationType(expirationType);
        expNoteAfterUpdate.setExpirationPeriod(expirationPeriod);
        expNoteAfterUpdate.setAvailable(isAvailable);


        long countOfNotesInReposBeforeUpdate = noteRepository.count();
        expNoteAfterUpdate.setExpirationFrom(LocalDateTime.now());
        NoteDto actualNoteAfterUpdate = noteService.updateNote(url, updateNoteRequestDto);


        assertNoteDtoEquals(expNoteAfterUpdate, actualNoteAfterUpdate);
        assertNoteExistsInRepository(actualNoteAfterUpdate, noteRepository);
        assertEquals(countOfNotesInReposBeforeUpdate, noteRepository.count());
        assertNoteExistsInCache(actualNoteAfterUpdate, noteCache);
    }

    @Test
    void updateTwoNotes() {

        addNotesInRepos(2);

        String url1 = "1";

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
        GetNoteRequestDto getNoteRequestDto1 = GetNoteRequestDto.builder()
                .url(url1)
                .build();

        NoteDto expNoteAfterUpdate1 = noteService.getNote(getNoteRequestDto1);
        expNoteAfterUpdate1.setTitle(title1);
        expNoteAfterUpdate1.setContent(content1);
        expNoteAfterUpdate1.setExpirationType(expirationType1);
        expNoteAfterUpdate1.setExpirationPeriod(expirationPeriod1);
        expNoteAfterUpdate1.setAvailable(isAvailable1);
        expNoteAfterUpdate1.setExpirationFrom(LocalDateTime.now());

        String url2 = "2";

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
        GetNoteRequestDto getNoteRequestDto2 = GetNoteRequestDto.builder()
                .url(url2)
                .build();

        NoteDto expNoteAfterUpdate2 = noteService.getNote(getNoteRequestDto2);
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

        // ----------------------------------------------------------------------------

        assertEquals(countOfNotesInReposBeforeUpdate, noteRepository.count());

        assertNoteDtoEquals(expNoteAfterUpdate1, actualNoteAfterUpdate1);
        assertNoteExistsInRepository(actualNoteAfterUpdate1, noteRepository);
        assertNoteExistsInCache(actualNoteAfterUpdate1, noteCache);

        assertNoteDtoEquals(expNoteAfterUpdate2, actualNoteAfterUpdate2);
        assertNoteExistsInRepository(actualNoteAfterUpdate2, noteRepository);
        assertNoteExistsInCache(actualNoteAfterUpdate2, noteCache);
    }

    @Test
    void updateNoteTwoTimes() {

        addNoteInRepos();

        String url = "1";

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

        GetNoteRequestDto getNoteRequestDto = GetNoteRequestDto.builder()
                .url(url)
                .build();
        NoteDto expNoteAfterUpdate = noteService.getNote(getNoteRequestDto);
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

        // ----------------------------------------------------------------------------

        assertEquals(countOfNotesInReposBeforeUpdate, noteRepository.count());

        assertNoteDtoEquals(expNoteAfterUpdate, actualNoteAfterUpdate);
        assertNoteExistsInRepository(actualNoteAfterUpdate, noteRepository);
        assertNoteExistsInCache(actualNoteAfterUpdate, noteCache);
    }

    @Test
    public void concurrentUpdateOfNote() {

        addNoteInRepos();

        String url = "1";

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

        GetNoteRequestDto getNoteRequestDto = GetNoteRequestDto.builder()
                .url(url)
                .build();
        NoteDto expNoteAfterUpdate1 = noteService.getNote(getNoteRequestDto);
        expNoteAfterUpdate1.setTitle(title1);
        expNoteAfterUpdate1.setContent(content1);
        expNoteAfterUpdate1.setExpirationType(expirationType1);
        expNoteAfterUpdate1.setExpirationPeriod(expirationPeriod1);
        expNoteAfterUpdate1.setAvailable(isAvailable1);

        NoteDto expNoteAfterUpdate2 = noteService.getNote(getNoteRequestDto);
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
        LocalDateTime expTimeOfExpirationFrom = LocalDateTime.now().plusSeconds(1);
        expNoteAfterUpdate1.setExpirationFrom(expTimeOfExpirationFrom);
        expNoteAfterUpdate2.setExpirationFrom(expTimeOfExpirationFrom);
        long countOfNotesInReposBeforeUpdate = noteRepository.count();
        combinedFuture.join();

        Optional<Note> updatedOptionalNoteInRep = noteRepository.findByUrl(url);
        NoteDto updatedNoteInRep = noteMapper.toDto(updatedOptionalNoteInRep.get());

        assertEquals(countOfNotesInReposBeforeUpdate, noteRepository.count());

        assertOneOfTwoNoteDto(updatedNoteInRep, expNoteAfterUpdate1, expNoteAfterUpdate2);
    }

    // validation ------------------------------------------------------------------------------------------------------

    @Test
    void allFieldsAreNull(){

        addNoteInRepos();

        String url = "1";
        UpdateNoteRequestDto updateNoteRequestDto = UpdateNoteRequestDto.builder()
                .title(null)
                .content(null)
                .expirationType(null)
                .expirationPeriod(null)
                .isAvailable(null)
                .build();

        Optional<Note> optionalNoteBeforeUpdateInRep = noteRepository.findByUrl(url);
        NoteDto noteBeforeUpdateInRep = noteMapper.toDto(optionalNoteBeforeUpdateInRep.get());


        long countOfNotesInReposBeforeUpdate = noteRepository.count();
        GroupValidationException groupOfExceptions = assertThrows(
                GroupValidationException.class,
                () -> {
                    noteService.updateNote(url, updateNoteRequestDto);
                }
        );
        List<? extends ValidationException> exceptions = groupOfExceptions.getExceptions();


        assertEquals(1, exceptions.size());
        assertEquals(ClientExceptionName.INVALID_UPDATE_NOTE_REQUEST_DTO, exceptions.get(0).getExceptionName());

        Optional<Note> updatedOptionalNoteInRep = noteRepository.findByUrl(url);
        NoteDto updatedNoteInRep = noteMapper.toDto(updatedOptionalNoteInRep.get());
        assertNoteDtoEquals(noteBeforeUpdateInRep, updatedNoteInRep);

        assertEquals(countOfNotesInReposBeforeUpdate, noteRepository.count());
    }

    @Test
    void invalidTitle(){

        addNoteInRepos();

        String url = "1";
        UpdateNoteRequestDto updateNoteRequestDto = UpdateNoteRequestDto.builder()
                .title(".Hello")
                .content(null)
                .expirationType(null)
                .expirationPeriod(null)
                .isAvailable(null)
                .build();

        Optional<Note> optionalNoteBeforeUpdateInRep = noteRepository.findByUrl(url);
        NoteDto noteBeforeUpdateInRep = noteMapper.toDto(optionalNoteBeforeUpdateInRep.get());


        long countOfNotesInReposBeforeUpdate = noteRepository.count();
        GroupValidationException groupOfExceptions = assertThrows(
                GroupValidationException.class,
                () -> {
                    noteService.updateNote(url, updateNoteRequestDto);
                }
        );
        List<? extends ValidationException> exceptions = groupOfExceptions.getExceptions();


        assertEquals(1, exceptions.size());
        assertEquals(ClientExceptionName.INVALID_TITLE, exceptions.get(0).getExceptionName());

        Optional<Note> updatedOptionalNoteInRep = noteRepository.findByUrl(url);
        NoteDto updatedNoteInRep = noteMapper.toDto(updatedOptionalNoteInRep.get());
        assertNoteDtoEquals(noteBeforeUpdateInRep, updatedNoteInRep);

        assertEquals(countOfNotesInReposBeforeUpdate, noteRepository.count());
    }

    @Test
    void invalidContent(){

        addNoteInRepos();

        String url = "1";
        UpdateNoteRequestDto updateNoteRequestDto = UpdateNoteRequestDto.builder()
                .title(null)
                .content(stringOfLength(contentLength + 1))
                .expirationType(null)
                .expirationPeriod(null)
                .isAvailable(null)
                .build();

        Optional<Note> optionalNoteBeforeUpdateInRep = noteRepository.findByUrl(url);
        NoteDto noteBeforeUpdateInRep = noteMapper.toDto(optionalNoteBeforeUpdateInRep.get());


        long countOfNotesInReposBeforeUpdate = noteRepository.count();
        GroupValidationException groupOfExceptions = assertThrows(
                GroupValidationException.class,
                () -> {
                    noteService.updateNote(url, updateNoteRequestDto);
                }
        );
        List<? extends ValidationException> exceptions = groupOfExceptions.getExceptions();


        assertEquals(1, exceptions.size());
        assertEquals(ClientExceptionName.INVALID_CONTENT, exceptions.get(0).getExceptionName());

        Optional<Note> updatedOptionalNoteInRep = noteRepository.findByUrl(url);
        NoteDto updatedNoteInRep = noteMapper.toDto(updatedOptionalNoteInRep.get());
        assertNoteDtoEquals(noteBeforeUpdateInRep, updatedNoteInRep);

        assertEquals(countOfNotesInReposBeforeUpdate, noteRepository.count());
    }

    @Test
    void invalidExpirationPeriod(){

        addNoteInRepos();

        Duration expirationPeriod = null;
        String url = "1";
        UpdateNoteRequestDto updateNoteRequestDto = UpdateNoteRequestDto.builder()
                .title(null)
                .content(null)
                .expirationType(ExpirationType.BURN_BY_PERIOD)
                .expirationPeriod(expirationPeriod)
                .isAvailable(null)
                .build();

        Optional<Note> optionalNoteBeforeUpdateInRep = noteRepository.findByUrl(url);
        NoteDto noteBeforeUpdateInRep = noteMapper.toDto(optionalNoteBeforeUpdateInRep.get());


        long countOfNotesInReposBeforeUpdate = noteRepository.count();
        GroupValidationException groupOfExceptions = assertThrows(
                GroupValidationException.class,
                () -> {
                    noteService.updateNote(url, updateNoteRequestDto);
                }
        );
        List<? extends ValidationException> exceptions = groupOfExceptions.getExceptions();


        assertEquals(1, exceptions.size());
        assertEquals(ClientExceptionName.INVALID_EXPIRATION_PERIOD, exceptions.get(0).getExceptionName());

        Optional<Note> updatedOptionalNoteInRep = noteRepository.findByUrl(url);
        NoteDto updatedNoteInRep = noteMapper.toDto(updatedOptionalNoteInRep.get());
        assertNoteDtoEquals(noteBeforeUpdateInRep, updatedNoteInRep);

        assertEquals(countOfNotesInReposBeforeUpdate, noteRepository.count());
    }

    @Test
    void allFieldsInvalid(){

        addNoteInRepos();

        Duration expirationPeriod = null;
        String url = "1";
        UpdateNoteRequestDto updateNoteRequestDto = UpdateNoteRequestDto.builder()
                .title(".Hello")
                .content(stringOfLength(contentLength + 1))
                .expirationType(ExpirationType.BURN_BY_PERIOD)
                .expirationPeriod(expirationPeriod)
                .isAvailable(null)
                .build();

        Optional<Note> optionalNoteBeforeUpdateInRep = noteRepository.findByUrl(url);
        NoteDto noteBeforeUpdateInRep = noteMapper.toDto(optionalNoteBeforeUpdateInRep.get());


        long countOfNotesInReposBeforeUpdate = noteRepository.count();
        GroupValidationException groupOfExceptions = assertThrows(
                GroupValidationException.class,
                () -> {
                    noteService.updateNote(url, updateNoteRequestDto);
                }
        );
        List<? extends ValidationException> exceptions = groupOfExceptions.getExceptions();


        assertEquals(3, exceptions.size());
        assertEquals(ClientExceptionName.INVALID_TITLE, exceptions.get(0).getExceptionName());
        assertEquals(ClientExceptionName.INVALID_CONTENT, exceptions.get(1).getExceptionName());
        assertEquals(ClientExceptionName.INVALID_EXPIRATION_PERIOD, exceptions.get(2).getExceptionName());

        Optional<Note> updatedOptionalNoteInRep = noteRepository.findByUrl(url);
        NoteDto updatedNoteInRep = noteMapper.toDto(updatedOptionalNoteInRep.get());
        assertNoteDtoEquals(noteBeforeUpdateInRep, updatedNoteInRep);

        assertEquals(countOfNotesInReposBeforeUpdate, noteRepository.count());
    }
}