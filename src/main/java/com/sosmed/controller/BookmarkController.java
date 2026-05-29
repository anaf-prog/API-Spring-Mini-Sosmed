package com.sosmed.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.sosmed.dto.ApiResponse;
import com.sosmed.dto.bookmark.BookmarkResponse;
import com.sosmed.security.CustomUserDetails;
import com.sosmed.service.BookmarkService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/bookmark")
@RequiredArgsConstructor
@Slf4j
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @PostMapping("/{postId}")
    public ResponseEntity<ApiResponse<Object>> toggleBookmark(
            @PathVariable("postId") Long postId,
            Authentication authentication) {

        // Validasi autentikasi
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("Autentikasi gagal. User tidak teridentifikasi saat mencoba membookmark postingan.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Silahkan login terlebih dahulu!");
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        ApiResponse<Object> response = bookmarkService.toggleBookmark(userId, postId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{postId}/users")
    public ResponseEntity<ApiResponse<List<BookmarkResponse>>> getPostBookmarks(
            @PathVariable("postId") Long postId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Authentication authentication) {

        // Validasi autentikasi
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("Autentikasi gagal saat mengakses daftar user bookmarks.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Silahkan login terlebih dahulu!");
        }

        ApiResponse<List<BookmarkResponse>> response = bookmarkService.getPostBookmarks(postId, page, size);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
}
