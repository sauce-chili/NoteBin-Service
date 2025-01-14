package vstu.isd.notebin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vstu.isd.notebin.cache.NoteCache;
import vstu.isd.notebin.dto.NoteViewRequestDto;
import vstu.isd.notebin.dto.NoteViewResponseDto;
import vstu.isd.notebin.dto.ViewAnalyticsDto;
import vstu.isd.notebin.entity.ViewNote;
import vstu.isd.notebin.exception.NoteNonExistsException;
import vstu.isd.notebin.mapper.NoteMapper;
import vstu.isd.notebin.repository.NoteRepository;
import vstu.isd.notebin.repository.ViewNoteRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ViewNoteRepository viewNoteRepository;

    private final NoteMapper noteMapper;

    private final NoteCache noteCache;

    private final NoteRepository noteRepository;

    @Transactional
    public NoteViewResponseDto createNoteView(NoteViewRequestDto noteViewRequestDto) {

        ViewNote viewNoteWithoutId = noteMapper.toViewNote(noteViewRequestDto);
        ViewNote viewNote = viewNoteRepository.save(viewNoteWithoutId);

        return noteMapper.toNoteViewResponseDto(viewNote);
    }

    public Map<String, ViewAnalyticsDto> getNotesViewAnalytics(List<String> urls) {

        return urls.stream()
                .collect(Collectors.toMap(
                        url -> url,
                        url -> getNoteViewAnalytics(getNoteId(url))
                ));
    }

    private Long getNoteId(String url) {
        return noteCache.get(url)
                .map(noteMapper::toDto)
                .orElseGet(() -> noteRepository.findByUrl(url)
                        .map(noteMapper::toDto)
                        .orElseThrow(() -> new NoteNonExistsException(url))
                )
                .getId();
    }

    private ViewAnalyticsDto getNoteViewAnalytics(Long id) {
        Set<ViewNote> viewsOfNote = viewNoteRepository.findByNoteId(id);

        long viewsFromNonAuthorized = viewsOfNote.stream()
                .filter(view -> view.getUserId() == null)
                .count();

        long viewsFromAuthorized = viewsOfNote.stream()
                .map(ViewNote::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        return new ViewAnalyticsDto(viewsFromAuthorized, viewsFromNonAuthorized);
    }
}
