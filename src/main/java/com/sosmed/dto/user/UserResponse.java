package com.sosmed.dto.user;

import java.time.LocalDateTime;

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
public class UserResponse {

    private Long id;
    private String username;
    private String fullname;
    private String email;
    private String image;
    private String bio;
    private Long followingCount;
    private Long followerCount;
    private Long postCount;
    private Long bookmarkCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
}
