package com.sosmed.dto.post;

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
    private PageResponse<PostCommentResponse> comments;
    
}
