package com.sosmed.dto.post;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PageResponse<T> {
    
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private List<T> content;
}
