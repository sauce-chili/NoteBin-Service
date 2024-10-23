package vstu.isd.notebin.repository;


import vstu.isd.notebin.entities.Note;

public interface NoteRepository {
    Iterable<Note> findAll();
    Note findById(int id);
    void save(Note note);
    void delete(Note note);
    Note findByUrl(String url);
    void update(Note note);
    Iterable<Note> findByAvailable(Boolean available);
}
