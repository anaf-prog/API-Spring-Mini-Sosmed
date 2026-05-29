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
public class LikeInfo {

    private Long postId;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private Long userId;
    private String fullname;
    private String username;
    private String userImage;
    
}
