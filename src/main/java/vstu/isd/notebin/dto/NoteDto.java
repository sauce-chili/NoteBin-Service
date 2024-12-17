package vstu.isd.notebin.dto;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import vstu.isd.notebin.entities.ExpirationType;

import java.time.OffsetDateTime;

@Builder(toBuilder = true)
@Data
@AllArgsConstructor
public class NoteDto {

    private Long id;

    @Size(max = 128)
    private String title;

    @NotNull
    private String noteText;

    private OffsetDateTime createAt;

    @NotNull
    private String url;

    private ExpirationType expirationType;
    private Boolean isAvailable;
}
