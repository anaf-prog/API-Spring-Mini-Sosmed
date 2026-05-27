package com.sosmed.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sosmed.dto.ApiResponse;
import com.sosmed.dto.auth.LoginRequest;
import com.sosmed.dto.auth.LoginResponse;
import com.sosmed.dto.auth.RegisterRequest;
import com.sosmed.dto.auth.RegisterResponse;
import com.sosmed.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> registerUser(@Valid @RequestBody RegisterRequest request) {

        ApiResponse<RegisterResponse> response = authService.resgister(request);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> loginUser(@Valid @RequestBody LoginRequest request) {
        
        ApiResponse<LoginResponse> response = authService.login(request);

        return new ResponseEntity<>(response, HttpStatus.OK);    
    }
    
}
