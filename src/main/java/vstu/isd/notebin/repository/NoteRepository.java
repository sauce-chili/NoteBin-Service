package vstu.isd.notebin.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vstu.isd.notebin.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.UnaryOperator;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {
    Optional<Note> findByUrl(String url);

    void deleteByUrl(String url);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Lock(LockModeType.PESSIMISTIC_READ)
    default Note updateWithLock(String url, UnaryOperator<Note> modifier) {
        return update(url, true, modifier);
    }

    private Note update(String url, boolean updateIfUnmodified, UnaryOperator<Note> modifier) {

        Note note = findByUrl(url).orElseThrow(NoSuchElementException::new);

        Note updated = modifier.apply(note);

        if (updateIfUnmodified || !note.equals(updated)) {
            return save(updated);
        }

        return updated;
    }

    Page<Note> findByUserId(Long userId, Pageable pageable);
}
