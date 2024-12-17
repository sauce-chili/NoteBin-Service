package vstu.isd.notebin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vstu.isd.notebin.entity.ExpirationType;

import java.time.Duration;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class CreateNoteRequestDto {
    private String title;
    private String content;
    private Boolean isAvailable;
    private ExpirationType expirationType;
    private Duration expirationPeriod;
}
