package vstu.isd.notebin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vstu.isd.notebin.cache.NoteCache;
import vstu.isd.notebin.dto.NoteViewRequestDto;
import vstu.isd.notebin.dto.NoteViewResponseDto;
import vstu.isd.notebin.dto.ViewAnalyticsDto;
import vstu.isd.notebin.entity.ViewNote;
import vstu.isd.notebin.mapper.NoteMapper;
import vstu.isd.notebin.repository.NoteRepository;
import vstu.isd.notebin.repository.ViewNoteRepository;

import java.security.InvalidParameterException;
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

        if (noteViewRequestDto.getNoteId() == null) {
            throw new InvalidParameterException("noteId in NoteViewRequest can't be null.");
        }

        Optional<ViewNote> viewNoteWithSameParameters =
                viewNoteRepository.findByNoteIdAndUserId(
                        noteViewRequestDto.getNoteId(),
                        noteViewRequestDto.getUserId()
                );

        NoteViewResponseDto viewNoteResponse;
        if (viewNoteWithSameParameters.isPresent()) {
            viewNoteResponse = noteMapper.toNoteViewResponseDto(viewNoteWithSameParameters.get());
        } else {
            ViewNote viewNoteWithoutId = noteMapper.toViewNote(noteViewRequestDto);
            ViewNote viewNote = viewNoteRepository.save(viewNoteWithoutId);
            viewNoteResponse = noteMapper.toNoteViewResponseDto(viewNote);
        }

        return viewNoteResponse;
    }

    public Map<String, Optional<ViewAnalyticsDto>> getNotesViewAnalytics(List<String> urls) {

        return urls.stream()
                .collect(Collectors.toMap(
                        url -> url,
                        url -> {
                            Long idOfNote = getNoteId(url);
                            return idOfNote == null ? Optional.empty() : Optional.ofNullable(getNoteViewAnalytics(idOfNote));
                        }
                ));
    }

    private Long getNoteId(String url) {
        return noteCache.get(url)
                .map(noteMapper::toDto)
                .orElseGet(() -> noteRepository.findByUrl(url)
                        .map(noteMapper::toDto)
                        .orElse(null)
                )
                != null ? noteCache.get(url).get().getId() : null;
    }

    private ViewAnalyticsDto getNoteViewAnalytics(Long noteId) {

        if (noteId == null) {
            return null;
        }

        return new ViewAnalyticsDto(
                viewNoteRepository.countOfAuthorizedViews(noteId),
                viewNoteRepository.countOfNonAuthorizedViews(noteId));
    }
}
