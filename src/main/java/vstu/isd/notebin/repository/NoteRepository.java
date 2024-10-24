package vstu.isd.notebin.repository;
import org.springframework.stereotype.Repository;
import vstu.isd.notebin.entities.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long>{
    Optional<Note> findByUrl(String url);
    void deleteByUrl(String url);
}
