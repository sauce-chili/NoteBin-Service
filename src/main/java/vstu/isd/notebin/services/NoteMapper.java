package vstu.isd.notebin.services;

import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;
import vstu.isd.notebin.entities.Note;
import vstu.isd.notebin.dto.NoteDto;

@Component
@Mapper(componentModel = "spring")
public interface NoteMapper {

    Note toNote(NoteDto noteDto);
    NoteDto toDto(Note note);

}
