package vstu.isd.notebin.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import vstu.isd.notebin.dto.GetNoteResponseDto;
import vstu.isd.notebin.dto.NoteDto;
import vstu.isd.notebin.entity.Note;
import vstu.isd.notebin.entity.BaseNote;
import vstu.isd.notebin.entity.NoteCacheable;

@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.FIELD,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface NoteMapper {

    @Mapping(source = "available", target = "isAvailable")
    NoteDto toDto(NoteCacheable noteCacheable);

    @Mapping(source = "available", target = "isAvailable")
    NoteDto toDto(Note note);

    @Mapping(source = "available", target = "isAvailable")
    NoteDto toDto(BaseNote baseNote);

    @Mapping(source = "available", target = "isAvailable")
    Note toEntity(NoteCacheable noteCacheable);

    @Mapping(source = "available", target = "isAvailable")
    NoteCacheable toCacheable(Note note);

    @Mapping(source = "available", target = "isAvailable")
    NoteCacheable toCacheable(NoteDto noteDto);

    @Mapping(source = "available", target = "isAvailable")
    GetNoteResponseDto toGetNoteResponseDto(NoteDto note);
}

