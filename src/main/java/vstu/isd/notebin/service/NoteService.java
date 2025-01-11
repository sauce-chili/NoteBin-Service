package vstu.isd.notebin.service;

import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vstu.isd.notebin.cache.NoteCache;
import vstu.isd.notebin.dto.CreateNoteRequestDto;
import vstu.isd.notebin.dto.GetNoteRequestDto;
import vstu.isd.notebin.dto.NoteDto;
import vstu.isd.notebin.dto.UpdateNoteRequestDto;
import vstu.isd.notebin.entity.BaseNote;
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

    private final RecalculateNoteAvailability recalculateNoteAvailabilityCommand;

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

        RecalculationAvailabilityResult recalculatedResult;
        try {
            recalculatedResult = recalculateNoteAvailabilityCommand.execute(note);
        } catch (NoSuchElementException e) {
            throw new NoteNonExistsException(getNoteRequestDto.getUrl());
        }

        if (recalculatedResult.isNotAvailableAfterRecalculation()) {
            throw new NoteUnavailableException(recalculatedResult.note().getUrl());
        }

        return recalculatedResult.note();
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

    @Transactional
    @Retryable(
            retryFor = {OptimisticLockException.class}
    )
    public NoteDto updateNote(String url, UpdateNoteRequestDto updateNoteRequest) {

        noteValidator.validateUpdateNoteRequestDto(updateNoteRequest).ifPresent( e -> {
            throw e;
        });

        LocalDateTime expirationFrom = LocalDateTime.now();
        updateNoteInCache(url, updateNoteRequest, expirationFrom);
        NoteDto updatedNote = updateNoteInRepository(url, updateNoteRequest, expirationFrom);

        return updatedNote;
    }

    private void updateNoteInCache(
            String url,
            UpdateNoteRequestDto updateNoteRequest,
            LocalDateTime expirationFrom
    ) {
        try {
            noteCache.update(url, cached -> {
                cached = noteMapper.fromUpdateRequest(cached, updateNoteRequest, expirationFrom);
                return cached;
            });
        } catch (NoSuchElementException ignored) {
        }
    }

    private NoteDto updateNoteInRepository(
            String url,
            UpdateNoteRequestDto updateNoteRequest,
            LocalDateTime expirationFrom
    ) {

        try {
            Note updated = noteRepository.updateWithLock(url, persisted -> {
                persisted = noteMapper.fromUpdateRequest(persisted, updateNoteRequest, expirationFrom);
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

    @Transactional
    @Retryable(
            maxAttempts = 5,
            backoff = @Backoff(delay = 50, multiplier = 1),
            retryFor = {OptimisticLockException.class}
    )
    public boolean deleteNote(String url) {

        NoteDto note = noteCache.get(url)
                .map(noteMapper::toDto)
                .orElseGet(
                        () -> noteRepository.findByUrl(url)
                                .map(noteMapper::toDto)
                                .orElseThrow(() -> new NoteNonExistsException(url))
                );

        if (!note.isAvailable()) {
            return true;
        }

        UpdateNoteRequestDto deleteNoteUpdate = UpdateNoteRequestDto.builder()
                .isAvailable(false)
                .build();

        updateNote(url, deleteNoteUpdate);

        return true;
    }
}

@Component
@RequiredArgsConstructor
class RecalculateNoteAvailability {

    private final NoteRepository noteRepository;
    private final NoteCache noteCache;
    private final NoteMapper noteMapper;

    /**
     * @throws NoSuchElementException  if note with given url does not exist
     * @throws OptimisticLockException if note was changed by another transaction
     **/
    public RecalculationAvailabilityResult execute(BaseNote note) {

        if (note.isNotAvailable()) {
            return new RecalculationAvailabilityResult(
                    noteMapper.toDto(note),
                    false
            );
        }

        UnaryOperator<BaseNote> noteModifier = switch (note.getExpirationType()) {
            case NEVER -> null;
            case BURN_AFTER_READ -> getChangeAvailabilityModifier();
            case BURN_BY_PERIOD -> {
                if (!note.isExpired()) {
                    yield null;
                }
                yield getChangeAvailabilityModifier();
            }
        };

        if (noteModifier == null) {
            return new RecalculationAvailabilityResult(
                    noteMapper.toDto(note),
                    note.isAvailable()
            );
        }

        Note changedNote = changeAvailabilityNote(
                note.getUrl(),
                noteModifier
        );

        boolean availableAfterRecalculation =
                changedNote.getExpirationType() == ExpirationType.BURN_AFTER_READ || changedNote.isAvailable();

        return new RecalculationAvailabilityResult(
                noteMapper.toDto(changedNote),
                availableAfterRecalculation
        );
    }

    /**
     * ATTENTION:
     * This modifier can change the availability of a note even if the note's expiration type has changed from its original value.
     * example: note was updated(in another request) from BURN_AFTER_READ to BURN_BY_PERIOD. In this case, the note will be burned if noted is expired.
     */
    private UnaryOperator<BaseNote> getChangeAvailabilityModifier() {
        return n -> {
            if (noteMustBeBurned(n)) {
                n.setAvailable(false);
                return n;
            }
            throw new OptimisticLockException();
        };
    }

    private boolean noteMustBeBurned(BaseNote n) {
        return switch (n.getExpirationType()) {
            case NEVER -> false;
            case BURN_AFTER_READ -> n.isAvailable();
            case BURN_BY_PERIOD -> n.isAvailable() && n.isExpired();
        };
    }

    private Note changeAvailabilityNote(
            String url,
            UnaryOperator<BaseNote> noteModifier
    ) {
        try {
            noteCache.update(url, n -> (NoteCacheable) noteModifier.apply(n));
        } catch (NoSuchElementException e) {
            // state of system changed
            throw new OptimisticLockException();
        }

        return noteRepository.updateWithLock(url, n -> (Note) noteModifier.apply(n));
    }
}

record RecalculationAvailabilityResult(
        NoteDto note,
        boolean isAvailableAfterRecalculation
) {
    public boolean isNotAvailableAfterRecalculation() {
        return !isAvailableAfterRecalculation;
    }
}