package com.sosmed.dto.follow;

import java.time.ZonedDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FollowResponse {
    
    private Long followerId;
    private Long followingId;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private UserFollowResponse user;
}
