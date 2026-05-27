package com.sosmed.dto.post;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserPostResponse {

    private Long id;
    private String fullname;
    private String username;
    private String image;
    
}
