package com.sosmed.Auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sosmed.controller.AuthController;
import com.sosmed.dto.auth.LoginRequest;
import com.sosmed.dto.auth.RegisterRequest;
import com.sosmed.security.JwtService;
import com.sosmed.service.AuthService;

@WebMvcTest(controllers = AuthController.class, excludeAutoConfiguration = { SecurityAutoConfiguration.class })
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("Register Berhasil")
    void register_Success() throws Exception {
        // Arrange: Register berhasil
        RegisterRequest request = RegisterRequest.builder()
            .email("test@mail.com")
            .username("testuser")
            .fullname("Test User")
            .password("Password123@")
            .build();

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated()) // Verifikasi status harus 201
            .andDo(MockMvcResultHandlers.print())
            .andReturn();

        // Cetak JSON Request
        String prettyJsonRequest = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        System.out.println("\n==============================================");
        System.out.println("Body :");
        System.out.println(prettyJsonRequest);
        System.out.println("==============================================");
    }

    @Test
    @DisplayName("Register Gagal - Password tidak memenuhi syarat (Harus 400 Bad Request)")
    void register_Failed_PasswordValidation() throws Exception {
        // Arrange: Password sengaja dibuat salah (tanpa huruf besar / karakter khusus)
        RegisterRequest request = RegisterRequest.builder()
            .email("test@mail.com")
            .username("testuser")
            .fullname("Test User")
            .password("password123")
            .build();

        // Act & Assert
        MvcResult result = mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest()) // Verifikasi status harus 400
            .andDo(MockMvcResultHandlers.print())
            .andReturn();

        // 1. Cetak JSON Request dengan format rapi (Pretty Print)
        String prettyJsonRequest = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        System.out.println("\n==============================================");
        System.out.println("Body :");
        System.out.println(prettyJsonRequest);
        System.out.println("==============================================");

        // 2. Ekstrak Exception dari Spring MVC untuk mengambil pesan error validasi
        Exception resolvedException = result.getResolvedException();
        if (resolvedException instanceof MethodArgumentNotValidException ex) {
            // Ambil error spesifik untuk field "password"
            FieldError fieldError = ex.getBindingResult().getFieldError("password");
            if (fieldError != null) {
                System.out.println("Error : " + fieldError.getDefaultMessage());
                System.out.println("==============================================\n");
            }
        }
    }

    @Test
    @DisplayName("Register Gagal - Username atau Fullname tidak boleh kosong")
    void register_Failed_UsernameValidation() throws Exception {
        // Arrange: Username atau fullname sengaja dibuat kosong
        RegisterRequest request = RegisterRequest.builder()
            .email("test@mail.com")
            .username(null)
            .fullname("Test User")
            .password("Password123@")
            .build();

        // Act & Assert
        MvcResult result = mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest()) // Verifikasi status harus 400
            .andDo(MockMvcResultHandlers.print())
            .andReturn();

        // 1. Cetak JSON Request dengan format rapi (Pretty Print)
        String prettyJsonRequest = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        System.out.println("\n==============================================");
        System.out.println("Body :");
        System.out.println(prettyJsonRequest);
        System.out.println("==============================================");

        // 2. Ekstrak Exception dari Spring MVC untuk mengambil pesan error validasi
        Exception resolvedException = result.getResolvedException();
        if (resolvedException instanceof MethodArgumentNotValidException ex) {
            // Ambil error spesifik untuk field "username"
            FieldError fieldError = ex.getBindingResult().getFieldError("username");
            if (fieldError != null) {
                System.out.println("Error : " + fieldError.getDefaultMessage());
                System.out.println("==============================================\n");
            }
        }
    }

    @Test
    @DisplayName("Register Gagal - Username Sudah Terdaftar")
    void register_Failed_UsernameExists() throws Exception {
        // 1. Arrange: Siapkan data request tiruan
        RegisterRequest request = RegisterRequest.builder()
            .email("anaf_baru@mail.com")
            .username("testuser") // Username ini disimulasikan sudah ada di DB
            .fullname("Anaf Test")
            .password("Password123@")
            .build();

        // 2. Mocking: Paksa authService melempar ResponseStatusException BAD_REQUEST saat method register dipanggil
        when(authService.resgister(any(RegisterRequest.class)))
            .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username sudah digunakan!"));

        // 3. Act & Assert: Lakukan hit POST, pastikan status kembalian adalah 400 Bad Request
        MvcResult result = mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest()) 
            .andDo(MockMvcResultHandlers.print())
            .andReturn();
        String prettyJsonRequest = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        
        Exception resolvedException = result.getResolvedException();
        String errorMessage = (resolvedException != null) ? resolvedException.getMessage() : "No Exception";

        System.out.println("\n================================================");
        System.out.println("REGISTER GAGAL - USERNAME SUDAH TERDAFTAR");
        System.out.println("================================================");
        System.out.println("DATA INPUT REQUEST:");
        System.out.println(prettyJsonRequest);
        System.out.println("------------------------------------------------");
        System.out.println("HASIL EXCEPTION SERVER:");
        System.out.println(errorMessage);
        System.out.println("================================================\n");
    }

    @Test
    @DisplayName("Login Berhasil")
    void login_Success() throws Exception {
        // Arrange: Login berhasil
        LoginRequest request = LoginRequest.builder()
            .usernameOrEmail("test@mail.com")
            .password("Password123@")
            .build();

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().is2xxSuccessful()) // Verifikasi status harus 200
            .andDo(MockMvcResultHandlers.print())
            .andReturn();

        // Cetak JSON Request
        String prettyJsonRequest = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        System.out.println("\n==============================================");
        System.out.println("Body :");
        System.out.println(prettyJsonRequest);
        System.out.println("==============================================");
    }

    @Test
    @DisplayName("Login Gagal - Username tidak ditemukan")
    void login_Failed_UsernameNotFound() throws Exception {
        // Arrange: Login berhasil
        LoginRequest request = LoginRequest.builder()
            .usernameOrEmail("test@mail.com")
            .password("Password123@")
            .build();

        when(authService.login(any(LoginRequest.class)))
            .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Username atau email : " + request.getUsernameOrEmail() + " tidak ditemukan"));    

        // Act & Assert
        MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andDo(MockMvcResultHandlers.print())
            .andReturn();

        // Cetak JSON Request
        String prettyJsonRequest = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);

        Exception resolvedException = result.getResolvedException();
        String errorMessage = (resolvedException != null) ? resolvedException.getMessage() : "No Exception";

        System.out.println("\n================================================");
        System.out.println("LOGIN GAGAL - USERNAME TIDAK DITEMUKAN");
        System.out.println("================================================");
        System.out.println("DATA INPUT REQUEST:");
        System.out.println(prettyJsonRequest);
        System.out.println("------------------------------------------------");
        System.out.println("HASIL EXCEPTION SERVER:");
        System.out.println(errorMessage);
        System.out.println("================================================\n");
    }

    @Test
    @DisplayName("Login Gagal - Password salah")
    void login_Failed_WrongPassword() throws Exception {
        // Arrange: Login berhasil
        LoginRequest request = LoginRequest.builder()
            .usernameOrEmail("test@mail.com")
            .password("Password123@")
            .build();

        when(authService.login(any(LoginRequest.class)))
            .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Password salah untuk user : " + request.getUsernameOrEmail()));

        // Act & Assert
        MvcResult result = mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andDo(MockMvcResultHandlers.print())
            .andReturn();

        // Cetak JSON Request
        String prettyJsonRequest = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);

        Exception resolvedException = result.getResolvedException();
        String errorMessage = (resolvedException != null) ? resolvedException.getMessage() : "No Exception";

        System.out.println("\n================================================");
        System.out.println("LOGIN GAGAL - PASSWORD SALAH");
        System.out.println("================================================");
        System.out.println("DATA INPUT REQUEST:");
        System.out.println(prettyJsonRequest);
        System.out.println("------------------------------------------------");
        System.out.println("HASIL EXCEPTION SERVER:");
        System.out.println(errorMessage);
        System.out.println("================================================\n");
    }
    
}
