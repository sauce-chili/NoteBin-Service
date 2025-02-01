package vstu.isd.notebin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vstu.isd.notebin.dto.*;
import vstu.isd.notebin.mapper.NoteMapper;
import vstu.isd.notebin.service.NoteService;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(
        name = "Note Controller",
        description = "Controller for managing notes"
)
@RestController
@RequestMapping("/api/v1/note")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;
    private final NoteMapper noteMapper;





    @Operation(
            summary = "Receiving a note",
            description = "Allows to get a note at a specified url.",
            security = @SecurityRequirement(name = "bearerAuth"),
            parameters = {
                    @Parameter(name = "url", description = "Unique identifier of the note.", required = true)
            }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Note found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = """
                                            {
                                                "url": "2bJ",
                                                "title": "1",
                                                "content": "2",
                                                "createdAt": [
                                                    2025,
                                                    1,
                                                    16,
                                                    20,
                                                    16,
                                                    50,
                                                    169684700
                                                ],
                                                "expirationType": "NEVER",
                                                "expirationPeriod": null,
                                                "available": true
                                            }"""
                            )
                    )),
            @ApiResponse(responseCode = "404",
                    description = """
                            Note wasn't get. It may be: \s
                            Note is expired (unavailable) (api error code 100), \s
                            Note with specified url is not found (api error code 101). \s
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                            example = """
                                    {
                                        "type": "error",
                                        "title": "Not Found",
                                        "status": 404,
                                        "detail": "Note with url JAJA not found",
                                        "instance": "/api/v1/note/JAJA",
                                        "properties": {
                                            "date": [
                                                2025,
                                                1,
                                                16,
                                                20,
                                                13,
                                                51,
                                                577134600
                                            ],
                                            "api_error_code": 101,
                                            "api_error_name": "NOTE_NOT_FOUND",
                                            "args": {
                                                "url": "JAJA"
                                            }
                                        }
                                    }"""
                            )
                    )
    ),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/{url}")
    public NoteResponseDto getNote(
            @PathVariable String url,
            @RequestAttribute(value = "x-user-id", required = false) Long userId
    ) {
        NoteDto noteDto = noteService.getNote(new GetNoteRequestDto(url, userId));

        return noteMapper.toNoteResponseDto(noteDto);
    }





    @Operation(
            summary = "Creates a note",
            description = "Allows to create note.",
            security = @SecurityRequirement(name = "bearerAuth"),
            parameters = {
                    @Parameter(name = "requestDto", description = "Parameters of note.", required = true)
            }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Note created.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = """
                                            {
                                                "url": "2bJ",
                                                "title": "1",
                                                "content": "2",
                                                "createdAt": [
                                                    2025,
                                                    1,
                                                    16,
                                                    20,
                                                    16,
                                                    50,
                                                    169684700
                                                ],
                                                "expirationType": "NEVER",
                                                "expirationPeriod": null,
                                                "available": true
                                            }"""
                            )
                    )),
            @ApiResponse(responseCode = "404",
                    description = """
                    Note wasn't created. It may be \s
                    Group of validation exceptions (api error code 801), \s
                    Validation exception: Invalid title of note (api error code 802), \s
                    Validation exception: Invalid content of note (api error code 803), \s
                    Validation exception: Invalid expiration type of note (api error code 804), \s
                    Validation exception: Invalid expiration period of note (api error code 805)""",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = """
                                            {
                                                "type": "error",
                                                "title": "Bad Request",
                                                "status": 400,
                                                "detail": "Group validation exception.",
                                                "instance": "/api/v1/note",
                                                "properties": {
                                                    "date": [
                                                        2025,
                                                        1,
                                                        16,
                                                        20,
                                                        30,
                                                        42,
                                                        59954600
                                                    ],
                                                    "api_error_code": 801,
                                                    "api_error_name": "GROUP_VALIDATION_EXCEPTION",
                                                    "args": {},
                                                    "errors": [
                                                        {
                                                            "api_error_code": 802,
                                                            "api_error_name": "INVALID_TITLE",
                                                            "args": {},
                                                            "detail": "Title mustn't contain only white delimiters. At the same time, the first symbol must be a digit or letter.Max length is 128 symbols."
                                                        },
                                                        {
                                                            "api_error_code": 805,
                                                            "api_error_name": "INVALID_EXPIRATION_PERIOD",
                                                            "args": {},
                                                            "detail": "Expiration period must be set when expiration type is BURN BY PERIOD"
                                                        }
                                                    ]
                                                }
                                            }"""
                            )
                    )),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    public NoteResponseDto createNote(
            @RequestBody CreateNoteRequestDto requestDto,
            @RequestAttribute(value = "x-user-id", required = false) Long userId
    ) {
        requestDto.setUserId(userId);
        NoteDto noteDto = noteService.createNote(requestDto);

        return noteMapper.toNoteResponseDto(noteDto);
    }





    @Operation(
            summary = "Updates a note",
            description = "Allows to update note with specified url.",
            security = @SecurityRequirement(name = "bearerAuth"),
            parameters = {
                    @Parameter(name = "requestDto", description = "Parameters of note for update. " +
                            "Fields that will not be changed are not set. At least one field must be changed.", required = true)
            }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Note updated.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = """
                                            {
                                                "url": "2bK",
                                                "title": "new title",
                                                "content": "2",
                                                "createdAt": [
                                                    2025,
                                                    1,
                                                    16,
                                                    20,
                                                    33,
                                                    19,
                                                    621273000
                                                ],
                                                "expirationType": "NEVER",
                                                "expirationPeriod": null,
                                                "available": true
                                            }"""
                            )
                    )),
            @ApiResponse(responseCode = "404", description = """
                    Note wasn't created. It may be: \s
                    Note is expired (unavailable) (api error code 100), \s
                    Note with specified url is not found (api error code 101). \s
                    Note update not allowed: It's belongs to other user (api error code 300), \s
                    Group of validation exceptions (api error code 801), \s
                    Validation exception: Invalid title of note (api error code 802), \s
                    Validation exception: Invalid content of note (api error code 803), \s
                    Validation exception: Invalid expiration period of note (api error code 805), \s
                    Validation exception: Empty update request (ape error code 806)""",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = """
                                            {
                                                "type": "error",
                                                "title": "Bad Request",
                                                "status": 400,
                                                "detail": "Group validation exception.",
                                                "instance": "/api/v1/note/2bK",
                                                "properties": {
                                                    "date": [
                                                        2025,
                                                        1,
                                                        16,
                                                        20,
                                                        34,
                                                        38,
                                                        988051600
                                                    ],
                                                    "api_error_code": 801,
                                                    "api_error_name": "GROUP_VALIDATION_EXCEPTION",
                                                    "args": {},
                                                    "errors": [
                                                        {
                                                            "api_error_code": 802,
                                                            "api_error_name": "INVALID_TITLE",
                                                            "args": {},
                                                            "detail": "Title mustn't contain only white delimiters. At the same time, the first symbol must be a digit or letter.Max length is 128 symbols."
                                                        },
                                                        {
                                                            "api_error_code": 805,
                                                            "api_error_name": "INVALID_EXPIRATION_PERIOD",
                                                            "args": {},
                                                            "detail": "Expiration period must be set when expiration type is BURN BY PERIOD"
                                                        }
                                                    ]
                                                }
                                            }"""
                            )
                    )),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/{url}")
    public NoteResponseDto updateNote(
            @PathVariable String url,
            @RequestBody UpdateNoteRequestDto requestDto,
            @RequestAttribute("x-user-id") Long userId
    ) {
        requestDto.setUserId(userId);
        NoteDto noteDto = noteService.updateNote(url, requestDto);

        return noteMapper.toNoteResponseDto(noteDto);
    }





    @Operation(
            summary = "Deactivates note",
            description = "Allows to deactivate note with specified url.",
            security = @SecurityRequirement(name = "bearerAuth"),
            parameters = {
                    @Parameter(name = "url", description = "Unique identifier of the note.", required = true)
            }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204",
                    description = "Note deactivated.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = "No content"
                            )
                    )),
            @ApiResponse(responseCode = "404", description = """
                    Note wasn't deactivated. It may be: \s
                    Note with specified url is not exists (api error code 101), \s
                    Note deactivate not allowed: It's belongs to other user (api error code 300).""",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = """
                                            {
                                                "type": "error",
                                                "title": "Not Found",
                                                "status": 404,
                                                "detail": "Note with url 2bJNVUJIDS not found",
                                                "instance": "/api/v1/note/2bJNVUJIDS",
                                                "properties": {
                                                    "date": [
                                                        2025,
                                                        1,
                                                        16,
                                                        20,
                                                        35,
                                                        40,
                                                        196057500
                                                    ],
                                                    "api_error_code": 101,
                                                    "api_error_name": "NOTE_NOT_FOUND",
                                                    "args": {
                                                        "url": "2bJNVUJIDS"
                                                    }
                                                }
                                            }"""
                            )
                    )),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PatchMapping("/{url}")
    public ResponseEntity<Object> deactivateNote(
            @PathVariable String url,
            @RequestAttribute("x-user-id") Long userId
    ) {
        noteService.deleteNote(url, userId);

        return ResponseEntity.noContent().build();
    }





    @Operation(
            summary = "Find all user's notes",
            description = "Allows get all user's notes.",
            security = @SecurityRequirement(name = "bearerAuth"),
            parameters = {
                    @Parameter(name = "url", description = "Unique identifier of the note.", required = true)
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
                                                "userId": 1,
                                                "page": {
                                                    "content": [
                                                        {
                                                            "url": "2TF",
                                                            "title": "1",
                                                            "content": "2",
                                                            "createdAt": [
                                                                2025,
                                                                1,
                                                                16,
                                                                19,
                                                                51,
                                                                30,
                                                                708584000
                                                            ],
                                                            "expirationType": "NEVER",
                                                            "expirationPeriod": null,
                                                            "available": false
                                                        },
                                                        {
                                                            "url": "2bJ",
                                                            "title": "1",
                                                            "content": "2",
                                                            "createdAt": [
                                                                2025,
                                                                1,
                                                                16,
                                                                20,
                                                                16,
                                                                50,
                                                                169685000
                                                            ],
                                                            "expirationType": "NEVER",
                                                            "expirationPeriod": null,
                                                            "available": true
                                                        },
                                                        {
                                                            "url": "2bK",
                                                            "title": "new title",
                                                            "content": "2",
                                                            "createdAt": [
                                                                2025,
                                                                1,
                                                                16,
                                                                20,
                                                                33,
                                                                19,
                                                                621273000
                                                            ],
                                                            "expirationType": "NEVER",
                                                            "expirationPeriod": null,
                                                            "available": false
                                                        }
                                                    ],
                                                    "page": 0,
                                                    "pageSize": 3,
                                                    "totalPages": 1,
                                                    "totalElements": 3
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
    @GetMapping("/list/me")
    public GetUserNotesResponseDto<NoteResponseDto> getMyNotes(
            @RequestParam(defaultValue = "0") int page,
            @RequestAttribute("x-user-id") Long userId
    ) {
        GetUserNotesResponseDto<NoteDto> userNotesDto = noteService.getUserNotes(
                new GetUserNotesRequestDto(userId, page)
        );

        PageResponseDto<NoteResponseDto> pageResponse = noteMapper.fromPageResponseDto(
                userNotesDto.getPage(),
                noteMapper::toNoteResponseDto
        );

        return GetUserNotesResponseDto.<NoteResponseDto>builder()
                .userId(userNotesDto.getUserId())
                .page(pageResponse)
                .build();
    }




    @Operation(
            summary = "Receiving a note preview",
            description = "Allows to get a note stored options at a specified url.",
            security = @SecurityRequirement(name = "bearerAuth"),
            parameters = {
                    @Parameter(name = "url", description = "Unique identifier of the note.", required = true)
            }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "Note preview found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = """
                                            "url": "5an",
                                                "expirationType": "NEVER",
                                                "expirationPeriod": null,
                                                "expirationFrom": null
                                            """
                            )
                    )),
            @ApiResponse(responseCode = "404",
                    description = """
                            Note preview wasn't get. It may be: \s
                            Note is expired (unavailable) (api error code 100), \s
                            Note with specified url is not found (api error code 101). \s
                            """,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = """
                                            "type": "error",
                                                "title": "Not Found",
                                                "status": 404,
                                                "detail": "Note with url 5ab not found",
                                                "instance": "/api/v1/note/preview/5ab",
                                                "properties": {
                                                    "date": [
                                                        2025,
                                                        2,
                                                        1,
                                                        20,
                                                        9,
                                                        25,
                                                        534363200
                                                    ],
                                                    "api_error_code": 101,
                                                    "api_error_name": "NOTE_NOT_FOUND",
                                                    "args": {
                                                        "url": "5ab"
                                                    }
                                                }
                                            """
                            )
                    )
            ),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/preview/{url}")
    public NotePreviewDto getNotePreview(@PathVariable String url) {
        return noteService.getNotePreview(url);
    }





    @Operation(
            summary = "Test for user authentication.",
            description = "Allows check auth token.",
            security = @SecurityRequirement(name = "bearerAuth"),
            parameters = {
                    @Parameter(name = "url", description = "Unique identifier of the note.", required = true)
            }
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200",
                    description = "User authorized.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = "No content"
                            )
                    )),
            @ApiResponse(responseCode = "403", description = "User not authorized.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    example = "No content"
                            )
                    )),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/v/{t}")
    public void testAuth(@PathVariable String t, @RequestAttribute("x-user-id") Long userId) {
        System.out.println("t = " + t);
        System.out.println("userId = " + userId);
    }
}
