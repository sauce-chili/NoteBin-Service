package vstu.isd.notebin.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import vstu.isd.notebin.dto.CreateNoteRequestDto;
import vstu.isd.notebin.dto.GetNoteResponseDto;
import vstu.isd.notebin.dto.NoteDto;
import vstu.isd.notebin.entity.ExpirationType;
import vstu.isd.notebin.entity.Note;
import vstu.isd.notebin.entity.BaseNote;
import vstu.isd.notebin.entity.NoteCacheable;

import java.time.LocalDateTime;

@Mapper(
        componentModel = "spring",
        injectionStrategy = InjectionStrategy.FIELD,
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        imports = {java.time.LocalDateTime.class}
)
public interface NoteMapper {

    @Mapping(source = "available", target = "isAvailable")
    NoteDto toDto(NoteCacheable noteCacheable);

    @Mapping(source = "available", target = "isAvailable")
    NoteDto toDto(Note note);

    @Mapping(source = "available", target = "isAvailable")
    Note toEntity(NoteCacheable noteCacheable);

    @Mapping(source = "available", target = "isAvailable")
    NoteCacheable toCacheable(Note note);

    @Mapping(source = "available", target = "isAvailable")
    NoteCacheable toCacheable(NoteDto noteDto);

    default <T extends BaseNote> T fromUpdateRequest(T persisted, UpdateNoteRequestDto updateRequest) {
        if (updateRequest.getTitle() != null) {
            persisted.setTitle(updateRequest.getTitle());
        }
        if (updateRequest.getContent() != null) {
            persisted.setContent(updateRequest.getContent());
        }
        if (updateRequest.getExpirationType() != null) {
            if (updateRequest.getExpirationType() == ExpirationType.BURN_BY_PERIOD) {
                persisted.setExpirationPeriod(updateRequest.getExpirationPeriod());
            }
            persisted.setExpirationType(updateRequest.getExpirationType());
            persisted.setExpirationFrom(LocalDateTime.now());
        }
        if (updateRequest.getIsAvailable() != null) {
            persisted.setAvailable(updateRequest.getIsAvailable());
        }
        return persisted;
    }

    @Mapping(source = "available", target = "isAvailable")
    GetNoteResponseDto toGetNoteResponseDto(NoteDto note);

    @Mapping(target = "id", expression = "java(null)")
    @Mapping(target = "url", expression = "java(url)")
    @Mapping(target = "createdAt", expression = "java(LocalDateTime.now())")
    @Mapping(target = "isAvailable", constant = "true")
    @Mapping(target = "expirationFrom", expression = "java(LocalDateTime.now())")
    Note toNote(CreateNoteRequestDto createNoteRequestDto, String url);
}