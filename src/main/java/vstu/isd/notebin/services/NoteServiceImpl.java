package vstu.isd.notebin.services;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vstu.isd.notebin.entities.Note;
import vstu.isd.notebin.repository.NoteRepository;
import vstu.isd.notebin.util.UrlGenerator;
import vstu.isd.notebin.dto.NoteDto;

@Service
@RequiredArgsConstructor
public class NoteServiceImpl {

    private final NoteRepository noteRepository;
    private final UrlGenerator urlGenerator;
    private final NoteMapper mapper;

    public String createNote(NoteDto noteDto) {
        Note note = mapper.toNote(noteDto);
        note.setUrl(urlGenerator.generateUrl());
        noteRepository.save(note);
        return note.getUrl();
    }


    public NoteDto getNoteByUrl(String url) {
        Note note = noteRepository.findByUrl(url)
                .orElseThrow(() -> new IllegalArgumentException("Note not found"));
        return mapper.toDto(note);
    }


    public void updateNote(NoteDto noteDto) {
        noteRepository.save(mapper.toNote(noteDto));
        //TODO
        // Проверить наличие обновляемой записи
        //TODO
    }



}
