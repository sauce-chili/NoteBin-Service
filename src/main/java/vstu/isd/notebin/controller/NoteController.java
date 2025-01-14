package vstu.isd.notebin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vstu.isd.notebin.dto.*;
import vstu.isd.notebin.mapper.NoteMapper;
import vstu.isd.notebin.service.NoteService;

@RestController
@RequestMapping("/api/v1/note")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;
    private final NoteMapper noteMapper;

    @GetMapping("/{url}")
    public NoteResponseDto getNote(
            @PathVariable String url,
            @RequestAttribute(value = "x-user-id", required = false) Long userId
    ) {
        NoteDto noteDto = noteService.getNote(new GetNoteRequestDto(url));

        return noteMapper.toNoteResponseDto(noteDto);
    }

    @PostMapping
    public NoteResponseDto createNote(
            @RequestBody CreateNoteRequestDto requestDto,
            @RequestAttribute(value = "x-user-id", required = false) Long userId
    ) {
        NoteDto noteDto = noteService.createNote(requestDto);

        return noteMapper.toNoteResponseDto(noteDto);
    }

    @PutMapping("/{url}")
    public NoteResponseDto updateNote(
            @PathVariable String url,
            @RequestBody UpdateNoteRequestDto requestDto,
            @RequestAttribute("x-user-id") Long userId
    ) {
        NoteDto noteDto = noteService.updateNote(url, requestDto);

        return noteMapper.toNoteResponseDto(noteDto);
    }

    @PatchMapping("/{url}")
    public ResponseEntity<Object> deactivateNote(
            @PathVariable String url,
            @RequestAttribute("x-user-id") Long userId
    ) {

        noteService.deleteNote(url);

        return ResponseEntity.noContent().build();
    }

    // added auth method; requires user to be authenticated
    @GetMapping("/list/me")
    public GetUserNotesResponseDto<NoteResponseDto> getMyNotes(
            @RequestParam(defaultValue = "0") int page,
            @RequestAttribute("x-user-id") Long userId
    ) {
        GetUserNotesResponseDto<NoteDto> userNotesDto = noteService.getUserNotes(
                new GetUserNotesRequestDto(userId, page)
        );

        PageResponseDto<NoteResponseDto> pageResponse = PageResponseDto.<NoteResponseDto>builder()
                .content(
                        userNotesDto.getPage().getContent().stream()
                                .map(noteMapper::toNoteResponseDto)
                                .toList()
                )
                .page(userNotesDto.getPage().getPage())
                .pageSize(userNotesDto.getPage().getPageSize())
                .totalPages(userNotesDto.getPage().getTotalPages())
                .totalElements(userNotesDto.getPage().getTotalElements())
                .build();

        return GetUserNotesResponseDto.<NoteResponseDto>builder()
                .userId(userNotesDto.getUserId())
                .page(pageResponse)
                .build();
    }

    @GetMapping("/v/{t}")
    public void testAuth(@PathVariable String t, @RequestAttribute("x-user-id") Long userId) {
        System.out.println("t = " + t);
        System.out.println("userId = " + userId);
    }
}
