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
import com.sosmed.security.CustomUserDetails;

import com.sosmed.dto.ApiResponse;
import com.sosmed.dto.post.PostDetailResponse;
import com.sosmed.dto.post.PostRequest;
import com.sosmed.dto.post.PostResponse;
import com.sosmed.service.PostService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/post")
@RequiredArgsConstructor
@Slf4j
public class PostController {

    private final PostService postService;
    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostResponse>> createPost(
            @RequestPart(value = "caption", required = false) String caption,
            @RequestPart(value = "image", required = false) MultipartFile image,
            Authentication authentication) {

        // Validasi autentikasi
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("Autentikasi gagal. User tidak terautentikasi.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Silahkan login terlebih dahulu!");
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        // Build PostRequest manual
        PostRequest request = PostRequest.builder()
            .caption(caption)
            .image(image)
            .build();

        ApiResponse<PostResponse> response = postService.createPost(userId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Menampilkan semua postingan milik user yang sedang login dengan Paginasi
     * URL Contoh: /api/post/me?page=0&size=10
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<PostResponse>>> getMyPosts(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("Autentikasi gagal.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Silahkan login terlebih dahulu!");
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        ApiResponse<List<PostResponse>> response = postService.getUserPosts(userId, page, size);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PutMapping(value = "/edit/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostResponse>> editPost(
            @PathVariable("id") Long postId,
            @RequestPart(value = "caption", required = false) String caption,
            @RequestPart(value = "image", required = false) MultipartFile image,
            Authentication authentication) {

        // Validasi autentikasi
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("Autentikasi gagal. User tidak terautentikasi.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Silahkan login terlebih dahulu!");
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        // Build PostRequest manual
        PostRequest request = PostRequest.builder()
            .caption(caption)
            .image(image)
            .build();

        ApiResponse<PostResponse> response = postService.editPost(postId, userId, request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Mengambil halaman linimasa / explore global Postingan milik semua orang.
     */
    @GetMapping("/all-feed")
    public ResponseEntity<ApiResponse<List<PostResponse>>> getGlobalFeed(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Authentication authentication) {

        // Validasi autentikasi
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("Autentikasi gagal saat mengakses global feed.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Silahkan login terlebih dahulu!");
        }

        ApiResponse<List<PostResponse>> response = postService.getGlobalFeed(page, size);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Mengambil satu detail postingan berdasarkan ID-nya beserta komentar terpaginasi.
     */
    @GetMapping("/detail/{id}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> getPostDetail(
            @PathVariable("id") Long postId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "5") int size,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("Autentikasi gagal saat membuka detail postingan.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Silahkan login terlebih dahulu!");
        }

        ApiResponse<PostDetailResponse> response = postService.getPostDetail(postId, page, size);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Hapus postingan berdasarkan ID 
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deletePost(
            @PathVariable("id") Long postId,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("Autentikasi gagal saat mencoba menghapus postingan.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Silahkan login terlebih dahulu!");
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        ApiResponse<String> response = postService.deletePost(postId, userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


}
