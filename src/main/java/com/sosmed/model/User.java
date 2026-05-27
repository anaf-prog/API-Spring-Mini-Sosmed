package com.sosmed.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User{

    private Long id;
    private String fullname;
    private String username;
    private String email;
    private String password;
    private String bio;
    private String image;
    private String imageId;
    private Long followerCount;
    private Long followingCount;
    private Long postCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
}
