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
import com.sosmed.dto.follow.FollowResponse;
import com.sosmed.security.CustomUserDetails;
import com.sosmed.service.FollowService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/follow")
@RequiredArgsConstructor
@Slf4j
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{followingId}")
    public ResponseEntity<ApiResponse<FollowResponse>> toggleFollow(
            Authentication authentication,
            @PathVariable("followingId") Long followingId) {

        // Validasi autentikasi
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("Autentikasi gagal. User tidak terautentikasi.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Silahkan login terlebih dahulu!");
        }

        // Mengambil ID pelopor aksi (Follower) dari session login context
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long followerId = userDetails.getId();

        log.info("User ID {} mencoba merubah status follow ke User ID {}", followerId, followingId);

        ApiResponse<FollowResponse> response = followService.toggleFollow(followerId, followingId);
        
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{userId}/followers")
    public ResponseEntity<ApiResponse<List<FollowResponse>>> getFollowers(
            Authentication authentication,
            @PathVariable("userId") Long userId,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size) {

        // Validasi autentikasi
        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("Autentikasi gagal. User tidak terautentikasi.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Silahkan login terlebih dahulu!");
        }

        log.info("Mengambil daftar followers untuk User ID: {}, Page: {}, Size: {}", userId, page, size);

        // Memanggil service follower
        ApiResponse<List<FollowResponse>> response = followService.getFollowersList(userId, page, size);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{userId}/following")
    public ResponseEntity<ApiResponse<List<FollowResponse>>> getFollowing(
            Authentication authentication,
            @PathVariable("userId") Long userId,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size) {

        if (authentication == null || !authentication.isAuthenticated()) {
            log.error("Autentikasi gagal. User tidak terautentikasi.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Silahkan login terlebih dahulu!");
        }

        log.info("Mengambil daftar following untuk User ID: {}, Page: {}, Size: {}", userId, page, size);

        ApiResponse<List<FollowResponse>> response = followService.getFollowingList(userId, page, size);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
}
