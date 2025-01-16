package vstu.isd.notebin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vstu.isd.notebin.entity.ViewNote;
import vstu.isd.notebin.repository.result.ViewNoteStatistics;

import java.util.Optional;

@Repository
public interface ViewNoteRepository extends JpaRepository<ViewNote, Long> {

    Optional<ViewNote> findById(Long id);

    @Query(value = "SELECT * FROM view_note WHERE note_id = :noteId AND user_id = :userId LIMIT 1", nativeQuery = true)
    Optional<ViewNote> findByNoteIdAndUserId(@Param("noteId") Long noteId, @Param("userId") Long userId);

    @Query("SELECT COUNT(*) FROM ViewNote v WHERE v.noteId = :noteId AND v.userId IS NULL")
    Long countOfNonAuthorizedViews(@Param("noteId") Long noteId);

    @Query("SELECT COUNT(*) FROM ViewNote v WHERE v.noteId = :noteId AND v.userId IS NOT NULL")
    Long countOfAuthorizedViews(@Param("noteId") Long noteId);

    @Query("""
            select new vstu.isd.notebin.repository.result.ViewNoteStatistics(
                           COUNT(CASE WHEN v.userId IS NULL THEN 1 END),
                           COUNT(CASE WHEN v.userId IS NOT NULL THEN 1 END)
                       )
            from ViewNote v
            where v.noteId = :noteId
            """)
    ViewNoteStatistics countOfAuthorizedAndNonAuthorizedViews(@Param("noteId") Long noteId);
}
