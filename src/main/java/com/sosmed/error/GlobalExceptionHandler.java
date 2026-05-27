package com.sosmed.error;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.sosmed.dto.ErrorResponse;

import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse<String>> handleResponseStatusException(ResponseStatusException exception) {
        ErrorResponse<String> response = ErrorResponse.<String>builder()
            .code(exception.getStatusCode().value())
            .status("FAILED")
            .errors(exception.getReason()) // Ini akan mengambil pesan
            .build();

        return new ResponseEntity<>(response, exception.getStatusCode());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse<String>> handleRuntimeException(RuntimeException exception) {
        // Log error secara lengkap di server agar kamu gampang nge-bugging
        log.error("Terjadi RuntimeException sistem: {}", exception.getMessage(), exception);

        ErrorResponse<String> response = ErrorResponse.<String>builder()
                .code(HttpStatus.INTERNAL_SERVER_ERROR.value()) // HTTP 500
                .status("FAILED")
                .errors(exception.getMessage()) // Mengambil pesan 
                .build();

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
}
