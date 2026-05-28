package com.sosmed.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserCommentResponse {
    
    private Long id;
    private String fullname;
    private String username;
    private String image;
}
