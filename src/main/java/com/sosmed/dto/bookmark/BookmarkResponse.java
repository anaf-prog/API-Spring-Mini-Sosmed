package com.sosmed.dto.bookmark;

import java.time.ZonedDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookmarkResponse {

    private Long postId;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private UserBookmarkResponse user;
    
}
