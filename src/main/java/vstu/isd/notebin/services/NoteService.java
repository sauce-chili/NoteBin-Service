package vstu.isd.notebin.services;

import vstu.isd.notebin.dto.NoteDto;

public interface NoteService {
    String addNote(NoteDto noteDto);
    NoteDto getNoteByUrl(String url);
    void updateNote(NoteDto noteDto);
    void deleteNote(NoteDto noteDto);
}
