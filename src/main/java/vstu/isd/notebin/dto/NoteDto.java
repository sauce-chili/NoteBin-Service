package vstu.isd.notebin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vstu.isd.notebin.entity.ExpirationType;

import java.time.Duration;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class NoteDto {
    private Long id;
    private String title;
    private String content;
    private String url;
    private LocalDateTime createdAt;
    private boolean isAvailable;
    private ExpirationType expirationType;
    private Duration expirationPeriod;
    private LocalDateTime expirationFrom;
    private Long userId;
}