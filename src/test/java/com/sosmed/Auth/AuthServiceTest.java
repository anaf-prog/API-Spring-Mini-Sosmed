package com.sosmed.Auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.sosmed.dto.ApiResponse;
import com.sosmed.dto.auth.LoginRequest;
import com.sosmed.dto.auth.LoginResponse;
import com.sosmed.dto.auth.RegisterRequest;
import com.sosmed.dto.auth.RegisterResponse;
import com.sosmed.model.User;
import com.sosmed.repository.UserRepository;
import com.sosmed.security.JwtService;
import com.sosmed.service.AuthService;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Nested
    @DisplayName("Pengujian Fungsi Register")
    class RegisterTest {

        @Test
        @DisplayName("Register Berhasil - Mengembalikan ApiResponse dengan Token")
        void register_Success() {
            // Arrange (Persiapan data & behavior Mock)
            RegisterRequest request = RegisterRequest.builder()
                .email("test@mail.com")
                .username("testuser")
                .fullname("Test User")
                .password("password123")
                .build();

            User savedUser = User.builder()
                .id(1L)
                .email("test@mail.com")
                .username("testuser")
                .fullname("Test User")
                .password("Encrypted_password123@")
                .createdAt(LocalDateTime.now())
                .build();

            when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
            when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
            when(passwordEncoder.encode(request.getPassword())).thenReturn("Encrypted_password123@");
            when(userRepository.creatUser(any(User.class))).thenReturn(savedUser);
            when(jwtService.generateToken(savedUser.getId())).thenReturn("mocked-jwt-token");

            // Act (Eksekusi metode)
            ApiResponse<RegisterResponse> response = authService.resgister(request);

            System.out.println("\n==============================================");
            System.out.println("           DATA INPUT (REQUEST)");
            System.out.println("==============================================");
            System.out.println("Email       : " + request.getEmail());
            System.out.println("Username    : " + request.getUsername());
            System.out.println("Fullname    : " + request.getFullname());
            System.out.println("Password    : " + request.getPassword());

            System.out.println("\n==============================================");
            System.out.println("          DATA OUTPUT (RESPON API)");
            System.out.println("==============================================");
            System.out.println("Status      : " + response.getStatus());
            System.out.println("Message     : " + response.getMessage());
            System.out.println("Token       : " + response.getToken());
            System.out.println("Email       : " + response.getData().getEmail());
            System.out.println("Username    : " + response.getData().getUsername());
            System.out.println("Fullname    : " + response.getData().getFullname());
            System.out.println("Created At  : " + response.getData().getCreatedAt());
            System.out.println("==============================================\n");

            // Assert (Verifikasi hasil)
            assertNotNull(response);
            assertEquals(HttpStatus.CREATED.value(), response.getStatus());
            assertEquals("User berhasil registrasi", response.getMessage());
            assertEquals("mocked-jwt-token", response.getToken());
            assertEquals("testuser", response.getData().getUsername());

            // Verifikasi interaksi dengan mock
            verify(userRepository, times(1)).existsByEmail(request.getEmail());
            verify(userRepository, times(1)).existsByUsername(request.getUsername());
            verify(userRepository, times(1)).creatUser(any(User.class));
        }

        @Test
        @DisplayName("Register Gagal - Email Sudah Terdaftar")
        void register_Failed_EmailExists() {
            // Arrange
            RegisterRequest request = RegisterRequest.builder()
                .email("exists@mail.com")
                .username("testuser")
                .fullname("Test User")
                .password("password123")
                .build();

            when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

            // Act & Assert
            ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
                authService.resgister(request);
            });

            System.out.println("================================================");
            System.out.println("           DATA INPUT (REQUEST)");
            System.out.println("================================================");
            System.out.println("Email       : " + request.getEmail());
            System.out.println("Username    : " + request.getUsername());
            System.out.println("Fullname    : " + request.getFullname());
            System.out.println("Password    : " + request.getPassword());
            System.out.println("=================================================");

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("Email sudah terdaftar!", exception.getReason());

            // Verifikasi bahwa proses berhenti setelah pengecekan email
            verify(userRepository, never()).existsByUsername(anyString());
            verify(userRepository, never()).creatUser(any(User.class));
        }

        @Test
        @DisplayName("Register Gagal - Username Sudah Digunakan")
        void register_Failed_UsernameExists() {
            // Arrange
            RegisterRequest request = RegisterRequest.builder()
                .email("test@mail.com")
                .username("exists_username")
                .fullname("Test User")
                .password("password123")
                .build();

            when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
            when(userRepository.existsByUsername(request.getUsername())).thenReturn(true);

            // Act & Assert
            ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
                authService.resgister(request);
            });

            System.out.println("================================================");
            System.out.println("           DATA INPUT (REQUEST)");
            System.out.println("================================================");
            System.out.println("Email       : " + request.getEmail());
            System.out.println("Username    : " + request.getUsername());
            System.out.println("Fullname    : " + request.getFullname());
            System.out.println("Password    : " + request.getPassword());
            System.out.println("=================================================");

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("Username sudah digunakan!", exception.getReason());

            verify(userRepository, never()).creatUser(any(User.class));
        }
    }

    @Nested
    @DisplayName("Pengujian Fungsi Login")
    class LoginTest {

        @Test
        @DisplayName("Login Berhasil - Mengembalikan ApiResponse dengan Token")
        void login_Success() {
            // Arrange
            LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("password123")
                .build();

            User existingUser = User.builder()
                .id(1L)
                .email("test@mail.com")
                .username("testuser")
                .fullname("Test User")
                .password("encrypted_password")
                .image(null) // Menstimulasi fallback Dicebear
                .bio("test")
                .createdAt(LocalDateTime.now())
                .build();

            when(userRepository.findByUsernameOrEmail(request.getUsernameOrEmail()))
                .thenReturn(Optional.of(existingUser));
            when(passwordEncoder.matches(request.getPassword(), existingUser.getPassword()))
                .thenReturn(true);
            when(jwtService.generateToken(existingUser.getId()))
                .thenReturn("mocked-jwt-token");

            // Act
            ApiResponse<LoginResponse> response = authService.login(request);

            System.out.println("==============================================");
            System.out.println("          DATA OUTPUT (RESPON API)");
            System.out.println("==============================================");
            System.out.println("Status      : " + response.getStatus());
            System.out.println("Message     : " + response.getMessage());
            System.out.println("Token       : " + response.getToken());
            System.out.println("Email       : " + response.getData().getEmail());
            System.out.println("Username    : " + response.getData().getUsername());
            System.out.println("Fullname    : " + response.getData().getFullname());
            System.out.println("Image       : " + response.getData().getImage());
            System.out.println("Bio         : " + response.getData().getBio());
            System.out.println("Created At  : " + response.getData().getCreatedAt());
            System.out.println("===============================================");

            // Assert
            assertNotNull(response);
            assertEquals(HttpStatus.OK.value(), response.getStatus());
            assertEquals("User berhasil login", response.getMessage());
            assertEquals("mocked-jwt-token", response.getToken());
            
            // Verifikasi logika fallback Dicebear berjalan saat image null
            String expectedFallbackImage = "https://api.dicebear.com/7.x/initials/svg?seed=" + existingUser.getUsername();
            assertEquals(expectedFallbackImage, response.getData().getImage());
        }

        @Test
        @DisplayName("Login Gagal - User Tidak Ditemukan")
        void login_Failed_UserNotFound() {
            // Arrange
            LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("nonexistent")
                .password("password123")
                .build();

            when(userRepository.findByUsernameOrEmail(request.getUsernameOrEmail()))
                .thenReturn(Optional.empty());

            // Act & Assert
            ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
                authService.login(request);
            });

            System.out.println("================================================");
            System.out.println("           DATA INPUT (REQUEST)");
            System.out.println("================================================");
            System.out.println("usernameOrEmail : " + request.getUsernameOrEmail());
            System.out.println("Password        : " + request.getPassword());
            System.out.println("=================================================");

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
            assertEquals("Username atau password salah!", exception.getReason());
        }

        @Test
        @DisplayName("Login Gagal - Password Salah")
        void login_Failed_InvalidPassword() {
            // Arrange
            LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("testuser")
                .password("wrongpassword")
                .build();

            User existingUser = User.builder()
                .id(1L)
                .password("encrypted_password")
                .build();

            when(userRepository.findByUsernameOrEmail(request.getUsernameOrEmail()))
                .thenReturn(Optional.of(existingUser));
            when(passwordEncoder.matches(request.getPassword(), existingUser.getPassword()))
                .thenReturn(false);

            // Act & Assert
            ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
                authService.login(request);
            });

            System.out.println("================================================");
            System.out.println("           DATA INPUT (REQUEST)");
            System.out.println("================================================");
            System.out.println("usernameOrEmail : " + request.getUsernameOrEmail());
            System.out.println("Password        : " + request.getPassword());
            System.out.println("=================================================");

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
            assertEquals("Username atau password salah!", exception.getReason());
        }
    }
    
}
