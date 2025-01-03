package vstu.isd.notebin.service;

import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vstu.isd.notebin.cache.NoteCache;
import vstu.isd.notebin.dto.GetNoteRequestDto;
import vstu.isd.notebin.dto.NoteDto;
import vstu.isd.notebin.entity.Note;
import vstu.isd.notebin.entity.NoteCacheable;
import vstu.isd.notebin.exception.NoteNonExistsException;
import vstu.isd.notebin.exception.NoteUnavailableException;
import vstu.isd.notebin.mapper.NoteMapper;
import vstu.isd.notebin.repository.NoteRepository;
import vstu.isd.notebin.service.ChangeNoteAvailabilityCommand.ChangeAvailabilityResult;

import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoteService {

    private final NoteRepository noteRepository;
    private final NoteCache noteCache;
    private final NoteMapper noteMapper;

    private final ChangeNoteAvailabilityCommand changeNoteAvailabilityCommand;

    @Transactional
    @Retryable(
            maxAttempts = 5,
            backoff = @Backoff(delay = 200, multiplier = 1),
            retryFor = {OptimisticLockException.class}
    )
    public NoteDto getNote(GetNoteRequestDto getNoteRequestDto) {
        NoteCacheable note = getNoteAndCachingIfNecessary(getNoteRequestDto.getUrl())
                .orElseThrow(
                        () -> new NoteNonExistsException(getNoteRequestDto.getUrl())
                );

        if (!note.isAvailable()) {
            throw new NoteUnavailableException(getNoteRequestDto.getUrl());
        }

        ChangeAvailabilityResult changeResult;
        try {
            changeResult = changeNoteAvailabilityCommand.execute(note);
        } catch (NoSuchElementException e) {
            throw new NoteNonExistsException(getNoteRequestDto.getUrl());
        }

        if (changeResult.isNotChanged()){
            return noteMapper.toDto(note);
        }

        if (changeResult.isNotAvailableAfterChange()) {
            throw new NoteUnavailableException(changeResult.note().getUrl());
        }

        return changeResult.note();
    }

    private Optional<NoteCacheable> getNoteAndCachingIfNecessary(String url) {
        Optional<NoteCacheable> noteCacheable = noteCache.getAndExpire(url);
        if (noteCacheable.isPresent()) {
            return noteCacheable;
        }

        Optional<Note> note = noteRepository.findByUrl(url);
        if (note.isPresent()) {
            NoteCacheable cacheable = noteMapper.toCacheable(note.get());
            noteCache.save(cacheable);
            return Optional.of(cacheable);
        }

        return Optional.empty();
    }
}