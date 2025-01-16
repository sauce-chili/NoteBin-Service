package vstu.isd.notebin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vstu.isd.notebin.dto.GetUserNotesRequestDto;
import vstu.isd.notebin.dto.GetUserNotesResponseDto;
import vstu.isd.notebin.dto.NoteDto;
import vstu.isd.notebin.dto.ViewAnalyticsDto;
import vstu.isd.notebin.mapper.NoteMapper;
import vstu.isd.notebin.service.AnalyticsService;
import vstu.isd.notebin.service.NoteService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final NoteService noteService;
    private final NoteMapper noteMapper;

    @GetMapping("/view-notes/me")
    public Map<String, ViewAnalyticsDto> getMyNotesAnalytics(
            @RequestParam(defaultValue = "0") int page,
            @RequestAttribute("x-user-id") Long userId
    ) {
        GetUserNotesResponseDto<NoteDto> userNotesDto = noteService.getUserNotes(
                new GetUserNotesRequestDto(userId, page)
        );

        List<NoteDto> notes = userNotesDto.getPage().getContent();
        List<String> urls = notes.stream().map(NoteDto::getUrl).toList();

        Map<String, Optional<ViewAnalyticsDto>> viewAnalyticsOptional = analyticsService.getNotesViewAnalytics(urls);

        return noteMapper.toMapStringToViewNote(viewAnalyticsOptional);
    }
}
