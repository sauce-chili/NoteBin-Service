package vstu.isd.notebin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vstu.isd.notebin.dto.*;
import vstu.isd.notebin.mapper.NoteMapper;
import vstu.isd.notebin.service.NoteService;

@RestController
@RequestMapping("/note")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;
    private final NoteMapper noteMapper;

    @GetMapping("/{url}")
    public NoteResponseDto getNote(@PathVariable String url) {
        NoteDto noteDto = noteService.getNote(new GetNoteRequestDto(url));

        return noteMapper.toGetNoteResponseDto(noteDto);
    }

    @PostMapping
    public NoteResponseDto createNote(@RequestBody CreateNoteRequestDto requestDto) {
        NoteDto noteDto = noteService.createNote(requestDto);

        return noteMapper.toGetNoteResponseDto(noteDto);
    }

    @PatchMapping("/{url}")
    public NoteResponseDto updateNote(
            @PathVariable String url,
            @RequestBody UpdateNoteRequestDto requestDto
    ) {
        NoteDto noteDto = noteService.updateNote(url, requestDto);

        return noteMapper.toGetNoteResponseDto(noteDto);
    }
}
