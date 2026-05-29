package com.sosmed.dto.like;

import java.time.ZonedDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LikeResponse {

    private Long postId;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private UserLikeResponse user;
}
