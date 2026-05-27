package com.sosmed.dto.auth;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterResponse {
    
    private Long id;
    private String username;
    private String fullname;
    private String email;
    private String image;
    private String bio;
    private LocalDateTime createdAt;
}
