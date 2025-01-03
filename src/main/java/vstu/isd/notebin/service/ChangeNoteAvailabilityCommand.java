package vstu.isd.notebin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vstu.isd.notebin.cache.NoteCache;
import vstu.isd.notebin.dto.NoteDto;
import vstu.isd.notebin.entity.BaseNote;
import vstu.isd.notebin.entity.ExpirationType;
import vstu.isd.notebin.entity.Note;
import vstu.isd.notebin.entity.NoteCacheable;
import vstu.isd.notebin.mapper.NoteMapper;
import vstu.isd.notebin.repository.NoteRepository;

import java.util.NoSuchElementException;
import java.util.function.UnaryOperator;

import jakarta.persistence.OptimisticLockException;

@Component
@RequiredArgsConstructor
public class ChangeNoteAvailabilityCommand {

    private final NoteRepository noteRepository;
    private final NoteCache noteCache;
    private final NoteMapper noteMapper;

    /**
     * @throws NoSuchElementException  if note with given url does not exist
     * @throws OptimisticLockException if note was changed by another transaction
     **/
    public ChangeAvailabilityResult execute(BaseNote note) {

        UnaryOperator<BaseNote> noteModifier =
                switch (note.getExpirationType()) {
                    case NEVER -> null;
                    case BURN_AFTER_READ -> n -> {
                        if (n.isAvailable() &&
                                n.getExpirationType() == ExpirationType.BURN_AFTER_READ
                        ) {
                            n.setAvailable(false);
                            return n;
                        }
                        throw new OptimisticLockException();
                    };
                    case BURN_BY_PERIOD -> {
                        if (!note.isExpired()) {
                            yield null;
                        }
                        yield n -> {
                            if (n.isAvailable() &&
                                    n.getExpirationType() == ExpirationType.BURN_BY_PERIOD &&
                                    n.isExpired()
                            ) {
                                n.setAvailable(false);
                                return n;
                            }
                            throw new OptimisticLockException();
                        };
                    }
                };

        if (noteModifier == null) {
            return ChangeAvailabilityResult.ofNotChanged();
        }

        Note changedNote = changeAvailabilityNote(
                note.getUrl(),
                noteModifier
        );

        boolean isAvailableAfterChange = switch (note.getExpirationType()) {
            case NEVER, BURN_AFTER_READ -> true;
            case BURN_BY_PERIOD -> changedNote.isAvailable();
        };

        NoteDto changedNoteDto = noteMapper.toDto(changedNote);

        return ChangeAvailabilityResult.ofChanged(changedNoteDto, isAvailableAfterChange);
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

        Note updated = noteRepository.updateWithLock(url, n -> (Note) noteModifier.apply(n));
        return updated;
    }

    public record ChangeAvailabilityResult(
            NoteDto note,
            boolean isChanged,
            boolean isAvailableAfterChange
    ) {
        public static ChangeAvailabilityResult ofNotChanged() {
            return new ChangeAvailabilityResult(null, false, false);
        }

        public static ChangeAvailabilityResult ofChanged(NoteDto note, boolean isAvailableAfterChange) {
            return new ChangeAvailabilityResult(note, true, isAvailableAfterChange);
        }
    }
}
