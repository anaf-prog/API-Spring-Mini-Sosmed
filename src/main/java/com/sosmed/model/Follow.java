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
public class Follow {

    private Long followerId;
    private Long followingId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt; 
    
}
