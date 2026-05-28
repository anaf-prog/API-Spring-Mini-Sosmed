package com.sosmed.dto.post;

import java.time.ZonedDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PostCommentResponse {

    private Long id;
    private String content;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private UserPostResponse user;
    
}
