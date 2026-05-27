package com.sosmed.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.sosmed.security.CustomUserDetails;

import com.sosmed.dto.ApiResponse;
import com.sosmed.dto.user.UserResponse;
import com.sosmed.dto.user.UserUpdateRequest;
import com.sosmed.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> currentUser(Authentication authentication) {

        // Jika tidak lolos validasi, langsung lemparkan exception agar ditangkap GlobalExceptionHandler
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Silahkan login terlebih dahulu!");
        }

        // AMBIL OBJEK UTAMA (PRINCIPAL) LALU CASTING KE KELAS CUSTOMUSERDETAILS
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        // Panggil method getId() yang sudah kamu buat di CustomUserDetails
        Long userId = userDetails.getId();

        // Ambil data user melalui service tetap menggunakan Long userId 
        ApiResponse<UserResponse> response = userService.getCurrentUser(userId);

        return new ResponseEntity<>(response, HttpStatus.OK);

    }

    /**
     * Endpoint PATCH untuk memperbarui profile user secara parsial dan opsional.
     * Menggunakan consumes MULTIPART_FORM_DATA_VALUE agar bisa menerima gabungan berkas file gambar dan teks.
     */
    @PatchMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            Authentication authentication,
            @ModelAttribute UserUpdateRequest request) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Silahkan login terlebih dahulu!");
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        // Panggil service update profil
        ApiResponse<UserResponse> response = userService.updateUser(userId, request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(
            Authentication authentication,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Silahkan login terlebih dahulu!");
        }        

        ApiResponse<List<UserResponse>> response = userService.getAllUsers(page, size);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    
}
