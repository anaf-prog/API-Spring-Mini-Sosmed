package com.sosmed.error;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import com.sosmed.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse<String>> handleValidationException(
        MethodArgumentNotValidException exception, HttpServletRequest request) { // Menambahkan HttpServletRequest untuk tahu detail URL

        // Ambil informasi Endpoint dan HTTP Method
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // Ambil nama Object DTO yang sedang divalidasi
        String objectName = exception.getBindingResult().getObjectName();

        // Kumpulkan semua field yang error untuk dicatat di log
        String allErrorsLog = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> String.format("[%s: %s]", error.getField(), error.getDefaultMessage()))
            .collect(Collectors.joining(", "));

        // Ambil pesan error pertama untuk dikembalikan
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String errorMessage = (fieldError != null) ? fieldError.getDefaultMessage() : "Other Error";

        log.error("Error -> HTTP {} {} | Object: '{}' | Detail Error: {}", method, uri, objectName, allErrorsLog);

        ErrorResponse<String> response = ErrorResponse.<String>builder()
            .code(HttpStatus.BAD_REQUEST.value()) // HTTP 400
            .status("FAILED")
            .errors(errorMessage)
            .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse<String>> handleResponseStatusException(ResponseStatusException exception, HttpServletRequest request) {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        log.error("Error -> HTTP {} {} ", method, uri);

        ErrorResponse<String> response = ErrorResponse.<String>builder()
            .code(exception.getStatusCode().value())
            .status("FAILED")
            .errors(exception.getReason()) // Ini akan mengambil pesan
            .build();

        return new ResponseEntity<>(response, exception.getStatusCode());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse<String>> handleRuntimeException(RuntimeException exception, HttpServletRequest request) {

        String uri = request.getRequestURI();
        String method = request.getMethod();

        log.error("Error -> HTTP {} {} ", method, uri);
        log.error("Terjadi RuntimeException sistem: {}", exception.getMessage(), exception);

        ErrorResponse<String> response = ErrorResponse.<String>builder()
            .code(HttpStatus.INTERNAL_SERVER_ERROR.value()) // HTTP 500
            .status("FAILED")
            .errors("Other Error")
            .build();

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
}
