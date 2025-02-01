package vstu.isd.notebin.mapper;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import vstu.isd.notebin.dto.*;
import vstu.isd.notebin.entity.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    NoteDto toDto(BaseNote baseNote);

    @Mapping(source = "available", target = "isAvailable")
    NoteCacheable toCacheable(Note note);

    default <S, D> PageResponseDto<D> fromPageResponseDto(PageResponseDto<S> page, Function<S, D> map) {
        return PageResponseDto.<D>builder()
                .page(page.getPage())
                .pageSize(page.getPageSize())
                .totalPages(page.getTotalPages())
                .totalElements(page.getTotalElements())
                .content(
                        page.getContent().stream()
                                .map(map)
                                .collect(Collectors.toList())
                )
                .build();
    }

    default <T extends BaseNote> T fromUpdateRequest(T persisted, UpdateNoteRequestDto updateRequest, LocalDateTime expirationFrom) {
        if (updateRequest.getTitle() != null) {
            persisted.setTitle(updateRequest.getTitle());
        }

        if (updateRequest.getContent() != null) {
            persisted.setContent(updateRequest.getContent());
        }

        if (updateRequest.getExpirationType() != null) {
            persisted.setExpirationType(updateRequest.getExpirationType());
            persisted.setExpirationPeriod(updateRequest.getExpirationPeriod());
            persisted.setExpirationFrom(
                    updateRequest.getExpirationType() == ExpirationType.BURN_BY_PERIOD ? expirationFrom : null
            );
        }

        if (updateRequest.getIsAvailable() != null) {
            persisted.setAvailable(updateRequest.getIsAvailable());
        }

        return persisted;
    }

    @Mapping(source = "available", target = "isAvailable")
    NoteResponseDto toNoteResponseDto(NoteDto note);

    default Note toNote(CreateNoteRequestDto createNoteRequestDto, String url) {
        LocalDateTime now = LocalDateTime.now();

        Note note = Note.builder()
                .id(null)
                .title(createNoteRequestDto.getTitle())
                .content(createNoteRequestDto.getContent())
                .createdAt(now)
                .url(url)
                .expirationType(createNoteRequestDto.getExpirationType())
                .expirationPeriod(createNoteRequestDto.getExpirationPeriod())
                .expirationFrom(null)
                .isAvailable(true)
                .userId(createNoteRequestDto.getUserId())
                .build();

        if (note.getExpirationType() == ExpirationType.BURN_BY_PERIOD) {
            note.setExpirationFrom(now);
        }

        return note;
    }

    @Mapping(target = "viewedAt", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "id", expression = "java(null)")
    ViewNote toViewNote(NoteViewRequestDto noteViewRequestDto);

    @Mapping(source = "noteId", target = "noteId")
    NoteViewResponseDto toNoteViewResponseDto(ViewNote viewNote);

    @Mapping(source = "available", target = "isAvailable")
    Note toNote(NoteDto noteDto);

    default Map<String, ViewAnalyticsDto> toMapStringToViewNote(Map<String, Optional<ViewAnalyticsDto>> noteMap) {

        return noteMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().orElse(null)
                ));
    }

    NotePreviewDto toNotePreviewDto(NoteCacheable noteCacheable);
}