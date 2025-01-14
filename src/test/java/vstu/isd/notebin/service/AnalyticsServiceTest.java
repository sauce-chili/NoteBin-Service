package vstu.isd.notebin.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import vstu.isd.notebin.config.TestContainersConfig;
import vstu.isd.notebin.dto.CreateNoteRequestDto;
import vstu.isd.notebin.dto.NoteDto;
import vstu.isd.notebin.dto.NoteViewRequestDto;
import vstu.isd.notebin.dto.ViewAnalyticsDto;
import vstu.isd.notebin.entity.ExpirationType;
import vstu.isd.notebin.mapper.NoteMapper;
import vstu.isd.notebin.repository.NoteRepository;
import vstu.isd.notebin.repository.ViewNoteRepository;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static vstu.isd.notebin.testutils.TestAsserts.assertViewAnalyticsDtoEquals;

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

    @Test
    void getViewAnalytics() {

        NoteDto noteDto = addNextNoteToRepository();

        analyticsService.createNoteView(new NoteViewRequestDto(1L, null));
        analyticsService.createNoteView(new NoteViewRequestDto(1L, 1L));
        analyticsService.createNoteView(new NoteViewRequestDto(1L, 2L));
        analyticsService.createNoteView(new NoteViewRequestDto(1L, null));
        analyticsService.createNoteView(new NoteViewRequestDto(1L, null));


        List<String> urls = new LinkedList<>();
        urls.add(noteDto.getUrl());
        Map<String, ViewAnalyticsDto> viewAnalyticsOfNotes = analyticsService.getNotesViewAnalytics(urls);


        ViewAnalyticsDto expectedAnalytics = ViewAnalyticsDto.builder()
                .viewsFromAuthorized(2L)
                .viewsFromNonAuthorized(3L)
                .build();

        assertViewAnalyticsDtoEquals(expectedAnalytics, viewAnalyticsOfNotes.get(noteDto.getUrl()));
    }
}