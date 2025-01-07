package vstu.isd.notebin.service;

import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vstu.isd.notebin.cache.NoteCache;
import vstu.isd.notebin.dto.CreateNoteRequestDto;
import vstu.isd.notebin.dto.GetNoteRequestDto;
import vstu.isd.notebin.dto.NoteDto;
import vstu.isd.notebin.dto.UpdateNoteRequestDto;
import vstu.isd.notebin.entity.ExpirationType;
import vstu.isd.notebin.entity.Note;
import vstu.isd.notebin.entity.NoteCacheable;
import vstu.isd.notebin.exception.NoteNonExistsException;
import vstu.isd.notebin.exception.NoteUnavailableException;
import vstu.isd.notebin.generator.UrlGenerator;
import vstu.isd.notebin.mapper.NoteMapper;
import vstu.isd.notebin.repository.NoteRepository;
import vstu.isd.notebin.validation.NoteValidator;

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
    private final UrlGenerator urlGenerator;
    private final NoteValidator noteValidator;

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

        UnaryOperator<Note> repoModifier = null;
        UnaryOperator<NoteCacheable> cacheModifier = null;

        switch (noteCacheable.getExpirationType()) {
            case BURN_AFTER_READ -> {
                cacheModifier = note -> {
                    if (note.isAvailable() && note.getExpirationType() == ExpirationType.BURN_AFTER_READ) {
                        note.setAvailable(false);
                        return note;
                    }
                    throw new OptimisticLockException();
                };
                repoModifier = note -> {
                    if (note.isAvailable() && note.getExpirationType() == ExpirationType.BURN_AFTER_READ) {
                        note.setAvailable(false);
                        return note;
                    }
                    throw new OptimisticLockException();
                };
            }
            case BURN_BY_PERIOD -> {
                if (!noteCacheable.isExpired()) {
                    return noteCacheable;
                }
                cacheModifier = note -> {
                    if (note.isAvailable() &&
                            note.getExpirationType() == ExpirationType.BURN_BY_PERIOD &&
                            note.isExpired()
                    ) {
                        note.setAvailable(false);
                        return note;
                    }
                    throw new OptimisticLockException();
                };
                repoModifier = note -> {
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
            case NEVER -> {
                return noteCacheable;
            }
        }

        return changeAvailabilityNote(noteCacheable.getUrl(), cacheModifier, repoModifier);
    }

    private NoteCacheable changeAvailabilityNote(
            String url,
            UnaryOperator<NoteCacheable> cacheModifier,
            UnaryOperator<Note> repositoryModifier
    ) {
        try {
            noteCache.update(url, cacheModifier);
        } catch (NoSuchElementException e) {
            // state of system changed
            throw new OptimisticLockException();
        }

        Note updated = noteRepository.updateWithLock(url, repositoryModifier);
        return noteMapper.toCacheable(updated);
    }

    @Retryable(
            retryFor = {OptimisticLockException.class}
    )
    public NoteDto updateNote(String url, UpdateNoteRequestDto updateNoteRequest) {

        LocalDateTime expirationFrom = LocalDateTime.now();

        noteValidator.validateUpdateNoteRequestDto(updateNoteRequest).ifPresent( e -> {
            throw e;
        });

        updateNoteInCache(url, updateNoteRequest, expirationFrom);
        NoteDto updatedNote = updateNoteInRepository(url, updateNoteRequest, expirationFrom);

        return updatedNote;
    }

    private void updateNoteInCache(String url, UpdateNoteRequestDto updateNoteRequest, LocalDateTime expirationFrom) {

        try {
            noteCache.update(url, cached -> {
                cached = noteMapper.fromUpdateRequest(cached, updateNoteRequest);
                if (updateNoteRequest.getExpirationType() != null) {
                    cached.setExpirationFrom(expirationFrom);
                }
                return cached;
            });
        } catch (NoSuchElementException ignored) {
        }
    }

    private NoteDto updateNoteInRepository(String url, UpdateNoteRequestDto updateNoteRequest, LocalDateTime expirationFrom) {

        try {
            Note updated = noteRepository.updateWithLock(url, persisted -> {
                persisted = noteMapper.fromUpdateRequest(persisted, updateNoteRequest);
                if (updateNoteRequest.getExpirationType() != null) {
                    persisted.setExpirationFrom(expirationFrom);
                }
                return persisted;
            });

            return noteMapper.toDto(updated);
        } catch (NoSuchElementException e) {
            throw new NoteNonExistsException(url);
        }
    }

    @Transactional
    public NoteDto createNote(CreateNoteRequestDto createNoteRequest) {

        noteValidator.validateCreateNoteRequestDto(createNoteRequest).ifPresent( e -> {
            throw e;
        });

        String url = urlGenerator.generateUrl();
        Note noteWithoutId = noteMapper.toNote(createNoteRequest, url);
        Note savedNote = noteRepository.save(noteWithoutId);
        noteCache.save(noteMapper.toCacheable(savedNote));

        return noteMapper.toDto(savedNote);
    }
}