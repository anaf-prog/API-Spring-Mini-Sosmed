package com.sosmed.dto.post;

import java.util.List;

import com.sosmed.dto.PagingResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PostDetailResponse {

    private PostResponse post;
    private List<PostCommentResponse> comments;
    private PagingResponse paging;
    
}
