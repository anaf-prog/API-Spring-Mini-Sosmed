package com.sosmed.dto.post;

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
public class PostCommentResponse {

    private Long id;
    private String content;
    private String image;
    private String imageId;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private UserPostResponse user;
    
}
