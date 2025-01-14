package vstu.isd.notebin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vstu.isd.notebin.entity.ViewNote;

import java.util.Set;

@Repository
public interface ViewNoteRepository extends JpaRepository<ViewNote, Long> {

    Set<ViewNote> findByNoteId(Long noteId);
}
