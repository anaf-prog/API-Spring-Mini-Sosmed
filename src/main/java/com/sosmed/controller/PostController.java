package com.sosmed.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.sosmed.security.CustomUserDetails;

import com.sosmed.dto.ApiResponse;
import com.sosmed.dto.post.PostRequest;
import com.sosmed.dto.post.PostResponse;
import com.sosmed.service.PostService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/post")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PostResponse>> createPost(@ModelAttribute PostRequest request, Authentication authentication) {
        
        // Validasi autentikasi: Jika tidak lolos, lemparkan exception ke GlobalExceptionHandler
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Silahkan login terlebih dahulu!");
        }

        // AMBIL OBJEK UTAMA (PRINCIPAL) LALU CASTING KE KELAS CUSTOMUSERDETAILS
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // Ambil ID user yang sedang login secara dinamis
        Long userId = userDetails.getId();

        // Jalankan service pembuatan postingan dengan mengoper userId yang sah
        ApiResponse<PostResponse> response = postService.createPost(userId, request);
        
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
