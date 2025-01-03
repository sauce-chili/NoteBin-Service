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
import vstu.isd.notebin.entity.BaseNote;
import vstu.isd.notebin.entity.ExpirationType;
import vstu.isd.notebin.entity.Note;
import vstu.isd.notebin.entity.NoteCacheable;
import vstu.isd.notebin.exception.NoteNonExistsException;
import vstu.isd.notebin.exception.NoteUnavailableException;
import vstu.isd.notebin.mapper.NoteMapper;
import vstu.isd.notebin.repository.NoteRepository;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.UnaryOperator;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoteService {

    private final NoteRepository noteRepository;
    private final NoteCache noteCache;
    private final NoteMapper noteMapper;

    // currently under refactoring
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

        try {
            note = changeNoteAvailabilityIfNeeded(note);
        } catch (NoSuchElementException e) {
            throw new NoteNonExistsException(getNoteRequestDto.getUrl());
        }

        if (note.getExpirationType() == ExpirationType.BURN_BY_PERIOD && note.isExpired()) {
            throw new NoteUnavailableException(getNoteRequestDto.getUrl());
        }

        return noteMapper.toDto(note);
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

    // Вынести это в паттерн команда
    private NoteCacheable changeNoteAvailabilityIfNeeded(NoteCacheable noteCacheable) {

        UnaryOperator<BaseNote> noteModifier = switch (noteCacheable.getExpirationType()) {
            case BURN_AFTER_READ -> note -> {
                if (note.isAvailable() && note.getExpirationType() == ExpirationType.BURN_AFTER_READ) {
                    note.setAvailable(false);
                    return note;
                }
                throw new OptimisticLockException();
            };
            case BURN_BY_PERIOD -> {
                if (!noteCacheable.isExpired()) {
                    yield null;
                }
                yield note -> {
                    if (note.isAvailable() &&
                            note.getExpirationType() == ExpirationType.BURN_BY_PERIOD &&
                            note.isExpired()
                    ) {
                        note.setAvailable(false);
                        return note;
                    }
                    throw new OptimisticLockException();
                };
            }
            case NEVER -> null;
        };

        if (noteModifier == null) {
            return noteCacheable;
        }

        return changeAvailabilityNote(noteCacheable.getUrl(), noteModifier);
    }

    private NoteCacheable changeAvailabilityNote(
            String url,
            UnaryOperator<BaseNote> noteModifier
    ) {
        try {
            noteCache.update(url, n -> (NoteCacheable) noteModifier.apply(n));
        } catch (NoSuchElementException e) {
            // state of system changed
            throw new OptimisticLockException();
        }

        Note updated = noteRepository.updateWithLock(url, n -> (Note) noteModifier.apply(n));
        return noteMapper.toCacheable(updated);
    }
}