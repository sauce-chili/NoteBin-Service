package vstu.isd.notebin.repository;
import org.springframework.stereotype.Repository;
import vstu.isd.notebin.entities.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Date;
import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long>{}
