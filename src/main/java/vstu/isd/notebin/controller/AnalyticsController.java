package vstu.isd.notebin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vstu.isd.notebin.dto.*;
import vstu.isd.notebin.mapper.NoteMapper;
import vstu.isd.notebin.service.AnalyticsService;

import java.util.Map;
import java.util.Optional;

@Tag(
        name = "Analytic service Controller",
        description = "Controller for watching analytic of views"
)
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final NoteMapper noteMapper;

    @Operation(
            summary = "Calculating a view analytic of user's notes.",
            description = "Allows get analytic of views of notes with required urls.",
            security = @SecurityRequirement(name = "bearerAuth"),
            parameters = {
                    @Parameter(name = "GetViewsNoteRequestDto", description = "Unique identifiers of the requirement notes analytics.", required = true),
                    @Parameter(name = "userId", description = "Unique identifier of user who requires an action to note. " +
                            "Gets automatically from bearer token.", required = true)
            }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "List of user's notes required.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = """
                                            {
                                                "37Z": {
                                                    "userViews": 0,
                                                    "anonymousViews": 10
                                                }
                                            }"""
                            )
                    )),
            @ApiResponse(responseCode = "401",
                    description = "Not authorized",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = "No content"
                            )
                    )),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
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
