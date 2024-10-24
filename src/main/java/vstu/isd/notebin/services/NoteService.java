package vstu.isd.notebin.services;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vstu.isd.notebin.entities.Note;
import vstu.isd.notebin.repository.NoteRepository;
import vstu.isd.notebin.util.UrlGenerator;

import java.util.List;
import java.util.Optional;

@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final UrlGenerator urlGenerator;

    @Autowired
    public NoteService(NoteRepository noteRepository, UrlGenerator urlGenerator) {
        this.noteRepository = noteRepository;
        this.urlGenerator = urlGenerator;
    }

    public Note createNote(Note note) {
        note.setUrl(urlGenerator.generateUrl());
        return noteRepository.save(note);
    }

    public List<Note> getAllNotes() {
        return noteRepository.findAll();
    }

    public Optional<Note> getNoteById(Long id) {
        return noteRepository.findById(id);
    }

    public Optional<Note> getNoteByUrl(String url) {
        return noteRepository.findByUrl(url);
    }

    public Note updateNote(Note note) {
        return noteRepository.save(note);
    }

    public void deleteNoteById(Long id) {
        noteRepository.deleteById(id);
    }

    public void deleteNoteByUrl(String url) {
        noteRepository.deleteByUrl(url);
    }

}
