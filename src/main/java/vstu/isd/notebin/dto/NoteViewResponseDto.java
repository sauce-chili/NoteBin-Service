package vstu.isd.notebin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class NoteViewResponseDto {

    private Long id;
    private Long noteId;
    private Long userId;
}
