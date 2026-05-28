package com.sosmed.dto.comment;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CommentResponse {

    private Long id;
    private Long postId;
    private String content;
    private String image;
    private String imageId;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private UserCommentResponse user;
    
}
