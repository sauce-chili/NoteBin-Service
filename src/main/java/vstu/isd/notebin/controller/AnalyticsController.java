package vstu.isd.notebin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vstu.isd.notebin.dto.*;
import vstu.isd.notebin.mapper.NoteMapper;
import vstu.isd.notebin.service.AnalyticsService;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final NoteMapper noteMapper;

    @GetMapping("/view-notes")
    public Map<String, ViewAnalyticsDto> getViewNotesAnalytics(
            @RequestBody GetViewsNoteRequestDto requestViews
    ) {
        Map<String, Optional<ViewAnalyticsDto>> viewAnalyticsOptional = analyticsService.getNotesViewAnalytics(
                requestViews.getUrls()
        );
        return noteMapper.toMapStringToViewNote(viewAnalyticsOptional);
    }
}
