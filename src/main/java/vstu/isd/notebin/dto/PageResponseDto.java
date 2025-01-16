package vstu.isd.notebin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class PageResponseDto<D> {
    private List<D> content;
    private int page;
    private long pageSize;
    private long totalPages;
    private long totalElements;
}