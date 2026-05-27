package com.sosmed.dto.user;

import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserUpdateRequest {
    
    private String fullname;
    private String username;
    private String password;
    private String email;
    private String bio;
    private MultipartFile image;
}
