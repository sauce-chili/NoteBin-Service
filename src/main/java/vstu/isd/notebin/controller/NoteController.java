package vstu.isd.notebin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import vstu.isd.notebin.dto.*;
import vstu.isd.notebin.mapper.NoteMapper;
import vstu.isd.notebin.service.NoteService;

@RestController
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;
    private final NoteMapper noteMapper;

    @GetMapping("/{url}")
    public GetNoteResponseDto getNote(@PathVariable String url) {
        NoteDto noteDto = noteService.getNote(new GetNoteRequestDto(url));

        return noteMapper.toGetNoteResponseDto(noteDto);
    }

    @PostMapping("/note")
    public GetNoteResponseDto createNote(@RequestBody CreateNoteRequestDto requestDto) {
        NoteDto noteDto = noteService.createNote(requestDto);

        return noteMapper.toGetNoteResponseDto(noteDto);
    }
}
