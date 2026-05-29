package com.sosmed.dto.like;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserLikeResponse {

    private Long id;
    private String fullname;
    private String username;
    private String image;
    
}
