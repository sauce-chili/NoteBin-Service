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
public class UpdateNoteRequestDto {
    private String title;
    private String content;
    private ExpirationType expirationType;
    private Duration expirationPeriod;
    private Boolean isAvailable;
    private Long userId;

    public boolean isEmpty(){
        return title == null && content == null && expirationType==null && expirationPeriod == null && isAvailable == null;
    }
}
