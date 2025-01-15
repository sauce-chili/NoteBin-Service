package vstu.isd.notebin.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import vstu.isd.notebin.config.TestContainersConfig;
import vstu.isd.notebin.dto.*;
import vstu.isd.notebin.entity.ExpirationType;
import vstu.isd.notebin.entity.ViewNote;
import vstu.isd.notebin.mapper.NoteMapper;
import vstu.isd.notebin.repository.NoteRepository;
import vstu.isd.notebin.repository.ViewNoteRepository;

import java.security.InvalidParameterException;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static vstu.isd.notebin.testutils.TestAsserts.*;

@SpringBootTest
@ContextConfiguration(initializers = TestContainersConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@Slf4j
class AnalyticsServiceTest {

    @Autowired
    private NoteService noteService;

    @Autowired
    private NoteRepository noteRepository;

    @SpyBean
    private AnalyticsService analyticsService;

    @SpyBean
    private ViewNoteRepository viewNoteRepository;

    @Autowired
    private NoteMapper noteMapper;

    private AtomicLong indexOfNote = new AtomicLong(0);
    private NoteDto addNextNoteToRepository() {

        NoteDto noteDto = noteService.createNote(
                CreateNoteRequestDto.builder()
                        .title("Test Title " + indexOfNote.get())
                        .content("Test Content " + indexOfNote.getAndIncrement())
                        .expirationType(ExpirationType.NEVER)
                        .build()
        );

        noteRepository.save(noteMapper.toNote(noteDto));

        return noteDto;
    }

    @Nested
    @DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
    class getNotesViewAnalyticsTest {
        @Test
        void getViewAnalytics() {

            NoteDto noteDto = addNextNoteToRepository();

            analyticsService.createNoteView(new NoteViewRequestDto(noteDto.getId(), null));
            analyticsService.createNoteView(new NoteViewRequestDto(noteDto.getId(), 1L));
            analyticsService.createNoteView(new NoteViewRequestDto(noteDto.getId(), 2L));
            analyticsService.createNoteView(new NoteViewRequestDto(noteDto.getId(), null));
            analyticsService.createNoteView(new NoteViewRequestDto(noteDto.getId(), null));


            List<String> urls = new LinkedList<>();
            urls.add(noteDto.getUrl());
            Map<String, Optional<ViewAnalyticsDto>> viewAnalyticsOfNotes = analyticsService.getNotesViewAnalytics(urls);

            ViewAnalyticsDto expectedAnalytics = ViewAnalyticsDto.builder()
                    .viewsFromAuthorized(2L)
                    .viewsFromNonAuthorized(3L)
                    .build();

            assertViewAnalyticsDtoEquals(expectedAnalytics, viewAnalyticsOfNotes.get(noteDto.getUrl()).get());
        }

        @Test
        void allViewsByNonAuthorizedUsers() {

            NoteDto noteDto = addNextNoteToRepository();

            analyticsService.createNoteView(new NoteViewRequestDto(noteDto.getId(), null));
            analyticsService.createNoteView(new NoteViewRequestDto(noteDto.getId(), null));
            analyticsService.createNoteView(new NoteViewRequestDto(noteDto.getId(), null));


            List<String> urls = new LinkedList<>();
            urls.add(noteDto.getUrl());
            Map<String, Optional<ViewAnalyticsDto>> viewAnalyticsOfNotes = analyticsService.getNotesViewAnalytics(urls);


            ViewAnalyticsDto expectedAnalytics = ViewAnalyticsDto.builder()
                    .viewsFromAuthorized(0L)
                    .viewsFromNonAuthorized(3L)
                    .build();

            assertViewAnalyticsDtoEquals(expectedAnalytics, viewAnalyticsOfNotes.get(noteDto.getUrl()).get());
        }

        @Test
        void allViewsByDifferentAuthorizedUsers() {

            NoteDto noteDto = addNextNoteToRepository();

            analyticsService.createNoteView(new NoteViewRequestDto(noteDto.getId(), 1L));
            analyticsService.createNoteView(new NoteViewRequestDto(noteDto.getId(), 2L));
            analyticsService.createNoteView(new NoteViewRequestDto(noteDto.getId(), 3L));


            List<String> urls = new LinkedList<>();
            urls.add(noteDto.getUrl());
            Map<String, Optional<ViewAnalyticsDto>> viewAnalyticsOfNotes = analyticsService.getNotesViewAnalytics(urls);


            ViewAnalyticsDto expectedAnalytics = ViewAnalyticsDto.builder()
                    .viewsFromAuthorized(3L)
                    .viewsFromNonAuthorized(0L)
                    .build();

            assertViewAnalyticsDtoEquals(expectedAnalytics, viewAnalyticsOfNotes.get(noteDto.getUrl()).get());
        }

        @Test
        void allViewsBySameAuthorizedUser() {

            NoteDto noteDto = addNextNoteToRepository();

            analyticsService.createNoteView(new NoteViewRequestDto(noteDto.getId(), 1L));
            analyticsService.createNoteView(new NoteViewRequestDto(noteDto.getId(), 1L));
            analyticsService.createNoteView(new NoteViewRequestDto(noteDto.getId(), 1L));


            List<String> urls = new LinkedList<>();
            urls.add(noteDto.getUrl());
            Map<String, Optional<ViewAnalyticsDto>> viewAnalyticsOfNotes = analyticsService.getNotesViewAnalytics(urls);


            ViewAnalyticsDto expectedAnalytics = ViewAnalyticsDto.builder()
                    .viewsFromAuthorized(1L)
                    .viewsFromNonAuthorized(0L)
                    .build();

            assertViewAnalyticsDtoEquals(expectedAnalytics, viewAnalyticsOfNotes.get(noteDto.getUrl()).get());
        }

        @Test
        void getViewAnalyticsTwoTimesInRow() {

            NoteDto noteDto = addNextNoteToRepository();

            analyticsService.createNoteView(new NoteViewRequestDto(noteDto.getId(), null));
            analyticsService.createNoteView(new NoteViewRequestDto(noteDto.getId(), 1L));
            analyticsService.createNoteView(new NoteViewRequestDto(noteDto.getId(), 2L));
            analyticsService.createNoteView(new NoteViewRequestDto(noteDto.getId(), null));
            analyticsService.createNoteView(new NoteViewRequestDto(noteDto.getId(), null));


            List<String> urls = new LinkedList<>();
            urls.add(noteDto.getUrl());
            analyticsService.getNotesViewAnalytics(urls);
            Map<String, Optional<ViewAnalyticsDto>> viewAnalyticsOfNotes = analyticsService.getNotesViewAnalytics(urls);


            ViewAnalyticsDto expectedAnalytics = ViewAnalyticsDto.builder()
                    .viewsFromAuthorized(2L)
                    .viewsFromNonAuthorized(3L)
                    .build();

            assertViewAnalyticsDtoEquals(expectedAnalytics, viewAnalyticsOfNotes.get(noteDto.getUrl()).get());
        }

        @Test
        void getAnalyticsOfTwoNotes() {

            NoteDto noteDtoFirst = addNextNoteToRepository();
            NoteDto noteDtoSecond = addNextNoteToRepository();

            analyticsService.createNoteView(new NoteViewRequestDto(noteDtoFirst.getId(), 1L));
            analyticsService.createNoteView(new NoteViewRequestDto(noteDtoSecond.getId(), null));
            analyticsService.createNoteView(new NoteViewRequestDto(noteDtoSecond.getId(), 1L));
            analyticsService.createNoteView(new NoteViewRequestDto(noteDtoFirst.getId(), null));
            analyticsService.createNoteView(new NoteViewRequestDto(noteDtoFirst.getId(), 7L));
            analyticsService.createNoteView(new NoteViewRequestDto(noteDtoFirst.getId(), 1L));
            analyticsService.createNoteView(new NoteViewRequestDto(noteDtoSecond.getId(), null));


            List<String> urls = new LinkedList<>();
            urls.add(noteDtoFirst.getUrl());
            urls.add(noteDtoSecond.getUrl());
            Map<String, Optional<ViewAnalyticsDto>> viewAnalyticsOfNotes = analyticsService.getNotesViewAnalytics(urls);


            ViewAnalyticsDto expectedAnalyticsFirst = ViewAnalyticsDto.builder()
                    .viewsFromAuthorized(2L)
                    .viewsFromNonAuthorized(1L)
                    .build();
            ViewAnalyticsDto expectedAnalyticsSecond = ViewAnalyticsDto.builder()
                    .viewsFromAuthorized(1L)
                    .viewsFromNonAuthorized(2L)
                    .build();

            assertViewAnalyticsDtoEquals(expectedAnalyticsFirst, viewAnalyticsOfNotes.get(noteDtoFirst.getUrl()).get());
            assertViewAnalyticsDtoEquals(expectedAnalyticsSecond, viewAnalyticsOfNotes.get(noteDtoSecond.getUrl()).get());
        }

        @Test
        void zeroViewsOfNote() {

            NoteDto noteDto = addNextNoteToRepository();

            List<String> urls = new LinkedList<>();
            urls.add(noteDto.getUrl());
            Map<String, Optional<ViewAnalyticsDto>> viewAnalyticsOfNotes = analyticsService.getNotesViewAnalytics(urls);


            ViewAnalyticsDto expectedAnalytics = ViewAnalyticsDto.builder()
                    .viewsFromAuthorized(0L)
                    .viewsFromNonAuthorized(0L)
                    .build();

            assertViewAnalyticsDtoEquals(expectedAnalytics, viewAnalyticsOfNotes.get(noteDto.getUrl()).get());
        }

        @Test
        void fewViewsFromFewSameUsers() {

            NoteDto noteDto = addNextNoteToRepository();

            analyticsService.createNoteView(new NoteViewRequestDto(noteDto.getId(), 1L));
            analyticsService.createNoteView(new NoteViewRequestDto(noteDto.getId(), 1L));
            analyticsService.createNoteView(new NoteViewRequestDto(noteDto.getId(), 2L));
            analyticsService.createNoteView(new NoteViewRequestDto(noteDto.getId(), 2L));
            analyticsService.createNoteView(new NoteViewRequestDto(noteDto.getId(), 1L));
            analyticsService.createNoteView(new NoteViewRequestDto(noteDto.getId(), 2L));


            List<String> urls = new LinkedList<>();
            urls.add(noteDto.getUrl());
            Map<String, Optional<ViewAnalyticsDto>> viewAnalyticsOfNotes = analyticsService.getNotesViewAnalytics(urls);


            ViewAnalyticsDto expectedAnalytics = ViewAnalyticsDto.builder()
                    .viewsFromAuthorized(2L)
                    .viewsFromNonAuthorized(0L)
                    .build();

            assertViewAnalyticsDtoEquals(expectedAnalytics, viewAnalyticsOfNotes.get(noteDto.getUrl()).get());
        }

        @Test
        void getViewAnalyticsOfOneNonExistingNote() {

            NoteDto noteDto = addNextNoteToRepository();

            List<String> urls = new LinkedList<>();
            urls.add(noteDto.getUrl() + 1);
            Map<String, Optional<ViewAnalyticsDto>> viewAnalyticsOfNotes = analyticsService.getNotesViewAnalytics(urls);

            assertTrue(viewAnalyticsOfNotes.get(urls.get(0)).isEmpty());
        }

        @Test
        void getViewAnalyticsOfNonExistingNoteBetweenExistingNotes() {

            NoteDto noteDtoFirst = addNextNoteToRepository();
            NoteDto noteDtoSecond = addNextNoteToRepository();

            analyticsService.createNoteView(new NoteViewRequestDto(noteDtoFirst.getId(), 1L));
            analyticsService.createNoteView(new NoteViewRequestDto(noteDtoFirst.getId(), 1L));
            analyticsService.createNoteView(new NoteViewRequestDto(noteDtoSecond.getId(), 2L));
            analyticsService.createNoteView(new NoteViewRequestDto(noteDtoSecond.getId(), null));
            analyticsService.createNoteView(new NoteViewRequestDto(noteDtoSecond.getId(), 1L));
            analyticsService.createNoteView(new NoteViewRequestDto(noteDtoSecond.getId(), 2L));

            List<String> urls = new LinkedList<>();
            urls.add(noteDtoFirst.getUrl());
            urls.add("nonExist");
            urls.add(noteDtoSecond.getUrl());
            Map<String, Optional<ViewAnalyticsDto>> viewAnalyticsOfNotes = analyticsService.getNotesViewAnalytics(urls);


            ViewAnalyticsDto expectedFirstAnalytics = ViewAnalyticsDto.builder()
                    .viewsFromAuthorized(1L)
                    .viewsFromNonAuthorized(0L)
                    .build();
            ViewAnalyticsDto expectedSecondAnalytics = ViewAnalyticsDto.builder()
                    .viewsFromAuthorized(2L)
                    .viewsFromNonAuthorized(1L)
                    .build();
            assertViewAnalyticsDtoEquals(expectedFirstAnalytics, viewAnalyticsOfNotes.get(noteDtoFirst.getUrl()).get());
            assertViewAnalyticsDtoEquals(expectedSecondAnalytics, viewAnalyticsOfNotes.get(noteDtoSecond.getUrl()).get());
            assertTrue(viewAnalyticsOfNotes.get("nonExist").isEmpty());
        }
    }

    @Nested
    @DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
    class CreateNoteViewTest {

        @Test
        void createNoteView() {

            Long noteId = 1L;
            Long userId = 3L;
            NoteViewRequestDto noteViewRequest = NoteViewRequestDto.builder()
                    .noteId(noteId)
                    .userId(userId)
                    .build();


            long countOfViewsInRepositoryBeforeAdd = viewNoteRepository.count();
            NoteViewResponseDto actual = analyticsService.createNoteView(noteViewRequest);
            ViewNote actualViewNoteInRepository = viewNoteRepository.findById(actual.getId()).get();
            long countOfViewsInRepositoryAfterAdd = viewNoteRepository.count();


            assertEquals(countOfViewsInRepositoryBeforeAdd + 1, countOfViewsInRepositoryAfterAdd);

            NoteViewResponseDto expected = NoteViewResponseDto.builder()
                    .id(actual.getId())
                    .noteId(noteId)
                    .userId(userId)
                    .build();
            assertNoteViewResponseDtoEquals(expected, actual);

            ViewNote expectedViewInRepository = ViewNote.builder()
                    .id(actual.getId())
                    .noteId(noteId)
                    .userId(userId)
                    .viewedAt(LocalDateTime.now())
                    .build();
            assertViewNoteEquals(expectedViewInRepository, actualViewNoteInRepository);
        }

        @Test
        void createViewWithSameNoteAndUserId() {

            Long noteId = 1L;
            Long userId = 1L;
            NoteViewRequestDto noteViewRequest = NoteViewRequestDto.builder()
                    .noteId(noteId)
                    .userId(userId)
                    .build();


            long countOfViewsInRepositoryBeforeAdd = viewNoteRepository.count();
            NoteViewResponseDto actual = analyticsService.createNoteView(noteViewRequest);
            ViewNote actualViewNoteInRepository = viewNoteRepository.findById(actual.getId()).get();
            long countOfViewsInRepositoryAfterAdd = viewNoteRepository.count();


            assertEquals(countOfViewsInRepositoryBeforeAdd + 1, countOfViewsInRepositoryAfterAdd);

            NoteViewResponseDto expected = NoteViewResponseDto.builder()
                    .id(actual.getId())
                    .noteId(noteId)
                    .userId(userId)
                    .build();
            assertNoteViewResponseDtoEquals(expected, actual);

            ViewNote expectedViewInRepository = ViewNote.builder()
                    .id(actual.getId())
                    .noteId(noteId)
                    .userId(userId)
                    .viewedAt(LocalDateTime.now())
                    .build();
            assertViewNoteEquals(expectedViewInRepository, actualViewNoteInRepository);
        }

        @Test
        void createViewWithNullUserId() {

            Long noteId = 1L;
            Long userId = null;
            NoteViewRequestDto noteViewRequest = NoteViewRequestDto.builder()
                    .noteId(noteId)
                    .userId(userId)
                    .build();


            long countOfViewsInRepositoryBeforeAdd = viewNoteRepository.count();
            NoteViewResponseDto actual = analyticsService.createNoteView(noteViewRequest);
            ViewNote actualViewNoteInRepository = viewNoteRepository.findById(actual.getId()).get();
            long countOfViewsInRepositoryAfterAdd = viewNoteRepository.count();


            assertEquals(countOfViewsInRepositoryBeforeAdd + 1, countOfViewsInRepositoryAfterAdd);

            NoteViewResponseDto expected = NoteViewResponseDto.builder()
                    .id(actual.getId())
                    .noteId(noteId)
                    .userId(userId)
                    .build();
            assertNoteViewResponseDtoEquals(expected, actual);

            ViewNote expectedViewInRepository = ViewNote.builder()
                    .id(actual.getId())
                    .noteId(noteId)
                    .userId(userId)
                    .viewedAt(LocalDateTime.now())
                    .build();
            assertViewNoteEquals(expectedViewInRepository, actualViewNoteInRepository);
        }

        @Test
        void createViewWithNullNoteId() {

            Long noteId = null;
            Long userId = 1L;
            NoteViewRequestDto noteViewRequest = NoteViewRequestDto.builder()
                    .noteId(noteId)
                    .userId(userId)
                    .build();


            long countOfViewsInRepositoryBeforeAdd = viewNoteRepository.count();
            InvalidParameterException exception = assertThrows(
                    InvalidParameterException.class,
                    () -> analyticsService.createNoteView(noteViewRequest)
            );
            long countOfViewsInRepositoryAfterAdd = viewNoteRepository.count();


            assertEquals(countOfViewsInRepositoryBeforeAdd, countOfViewsInRepositoryAfterAdd);
        }

        @Test
        void createTwoNoteViewForOneNoteFromDifferentUsers() {

            Long noteIdFirst = 64L;
            Long userIdFirst = 87L;
            NoteViewRequestDto noteViewRequest = NoteViewRequestDto.builder()
                    .noteId(noteIdFirst)
                    .userId(userIdFirst)
                    .build();

            Long noteIdSecond = 90L;
            Long userIdSecond = 56L;
            NoteViewRequestDto noteViewRequestSecond = NoteViewRequestDto.builder()
                    .noteId(noteIdSecond)
                    .userId(userIdSecond)
                    .build();


            long countOfViewsInRepositoryBeforeAdd = viewNoteRepository.count();
            NoteViewResponseDto actualFirst = analyticsService.createNoteView(noteViewRequest);
            ViewNote actualFirstViewNoteInRepository = viewNoteRepository.findById(actualFirst.getId()).get();
            NoteViewResponseDto actualSecond = analyticsService.createNoteView(noteViewRequestSecond);
            ViewNote actualSecondViewNoteInRepository = viewNoteRepository.findById(actualSecond.getId()).get();
            long countOfViewsInRepositoryAfterAdd = viewNoteRepository.count();


            assertEquals(countOfViewsInRepositoryBeforeAdd + 2, countOfViewsInRepositoryAfterAdd);

            NoteViewResponseDto expectedFirst = NoteViewResponseDto.builder()
                    .id(actualFirst.getId())
                    .noteId(noteIdFirst)
                    .userId(userIdFirst)
                    .build();
            assertNoteViewResponseDtoEquals(expectedFirst, actualFirst);

            ViewNote expectedFirstViewInRepository = ViewNote.builder()
                    .id(actualFirst.getId())
                    .noteId(noteIdFirst)
                    .userId(userIdFirst)
                    .viewedAt(LocalDateTime.now())
                    .build();
            assertViewNoteEquals(expectedFirstViewInRepository, actualFirstViewNoteInRepository);

            NoteViewResponseDto expectedSecond = NoteViewResponseDto.builder()
                    .id(actualSecond.getId())
                    .noteId(noteIdSecond)
                    .userId(userIdSecond)
                    .build();
            assertNoteViewResponseDtoEquals(expectedSecond, actualSecond);

            ViewNote expectedSecondViewInRepository = ViewNote.builder()
                    .id(actualSecond.getId())
                    .noteId(noteIdSecond)
                    .userId(userIdSecond)
                    .viewedAt(LocalDateTime.now())
                    .build();
            assertViewNoteEquals(expectedSecondViewInRepository, actualSecondViewNoteInRepository);
        }
    }
}