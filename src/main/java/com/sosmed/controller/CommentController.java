package com.sosmed.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.sosmed.dto.ApiResponse;
import com.sosmed.dto.comment.CommentRequest;
import com.sosmed.dto.comment.CommentResponse;
import com.sosmed.security.CustomUserDetails;
import com.sosmed.service.CommentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/comment")
@RequiredArgsConstructor
@Slf4j
public class CommentController {

    private final CommentService commentService;

    @PostMapping(value = "/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @PathVariable("postId") Long postId,
            @RequestPart(value = "content", required = false) String content,
            @RequestPart(value = "image", required = false) MultipartFile image,
            Authentication authentication) {

        // Validasi autentikasi
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("Autentikasi gagal. User tidak terautentikasi.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Silahkan login terlebih dahulu!");
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        CommentRequest request = CommentRequest.builder()
            .content(content)
            .image(image)
            .build();

        ApiResponse<CommentResponse> response = commentService.createComment(userId, postId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping(value = "/edit/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CommentResponse>> editPost(
            @PathVariable("id") Long commentId,
            @RequestPart(value = "content", required = false) String content,
            @RequestPart(value = "image", required = false) MultipartFile image,
            Authentication authentication) {

        // Validasi autentikasi
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("Autentikasi gagal. User tidak terautentikasi.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Silahkan login terlebih dahulu!");
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        // Build CommentRequest manual
        CommentRequest request = CommentRequest.builder()
            .content(content)
            .image(image)
            .build();

        ApiResponse<CommentResponse> response = commentService.editComment(commentId, userId, request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Mengambil semua riwayat komentar milik user yang sedang login (Terpaginasi)
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getUserCommentHistory(
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("Autentikasi gagal saat mengakses riwayat komentar.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Silahkan login terlebih dahulu!");
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        ApiResponse<List<CommentResponse>> response = commentService.getUserCommentHistory(userId, page, size);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Hapus komentar berdasarkan ID 
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteComment(
            @PathVariable("id") Long commentId,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("Autentikasi gagal saat mencoba menghapus komentar.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Silahkan login terlebih dahulu!");
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        ApiResponse<String> response = commentService.deleteComment(commentId, userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
}
