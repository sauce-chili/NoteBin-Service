package vstu.isd.notebin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vstu.isd.notebin.dto.*;
import vstu.isd.notebin.mapper.NoteMapper;
import vstu.isd.notebin.repository.NoteRepository;
import vstu.isd.notebin.service.NoteService;

@RestController
@RequestMapping("/note")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;
    private final NoteMapper noteMapper;

    @GetMapping("/{url}")
    public GetNoteResponseDto getNote(@PathVariable String url) {
        NoteDto noteDto = noteService.getNote(new GetNoteRequestDto(url));

        return noteMapper.toGetNoteResponseDto(noteDto);
    }

    @PostMapping
    public GetNoteResponseDto createNote(@RequestBody CreateNoteRequestDto requestDto) {
        NoteDto noteDto = noteService.createNote(requestDto);

        return noteMapper.toGetNoteResponseDto(noteDto);
    }

    @PutMapping("/{url}")
    public GetNoteResponseDto updateNote(@PathVariable String url,
                                         @RequestBody UpdateNoteRequestDto requestDto) {
        NoteDto noteDto = noteService.updateNote(url, requestDto);

        return noteMapper.toGetNoteResponseDto(noteDto);
    }

    @PatchMapping("/{url}")
    public ResponseEntity<Object> deactivateNote(@PathVariable String url) {

        noteService.deleteNote(url);

        return ResponseEntity.noContent().build();
    }
}
