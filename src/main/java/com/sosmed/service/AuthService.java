package com.sosmed.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sosmed.dto.ApiResponse;
import com.sosmed.dto.auth.LoginRequest;
import com.sosmed.dto.auth.LoginResponse;
import com.sosmed.dto.auth.RegisterRequest;
import com.sosmed.dto.auth.RegisterResponse;
import com.sosmed.model.User;
import com.sosmed.repository.UserRepository;
import com.sosmed.security.JwtService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public ApiResponse<RegisterResponse> resgister(RegisterRequest request) {

        // Cek apakah email sudah digunakan
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Pendaftaran gagal: Email {} sudah terdaftar", request.getEmail());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email sudah terdaftar!");
        }

        // Cek apakah username sudah digunakan
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Pendaftaran gagal: Username {} sudah digunakan", request.getUsername());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username sudah digunakan!");
        }

        // 1. Map dari UserRequest ke User dan lakukan Enkripsi Password
        User userToSave = User.builder()
            .email(request.getEmail())
            .username(request.getUsername())
            .fullname(request.getFullname())
            .password(passwordEncoder.encode(request.getPassword()))
            .build();

        // 2. Simpan objek ke database melalui JDBC
        User savedUser = userRepository.creatUser(userToSave);

        log.info("Berhasil buat user");

        // 3. Generate token JWT untuk user berdasarkan ID yang dihasilkan database
        String token = jwtService.generateToken(savedUser.getId());

        // 4. Build response
        RegisterResponse registerResponse = RegisterResponse.builder()
            .id(savedUser.getId())
            .email(savedUser.getEmail())
            .fullname(savedUser.getFullname())
            .username(savedUser.getUsername())
            .image(savedUser.getImage())
            .bio(savedUser.getBio())
            .createdAt(savedUser.getCreatedAt())
            .build();

        // 5. Return langsung berupa ApiResponse global beserta tokennya
        return ApiResponse.<RegisterResponse>builder()
            .status(HttpStatus.CREATED.value())
            .message("User berhasil registrasi")
            .data(registerResponse)
            .token(token)
            .build();
    }

    public ApiResponse<LoginResponse> login(LoginRequest request) {
        // Cari data user berdasarkan username atau email
        User user = userRepository.findByUsernameOrEmail(request.getUsernameOrEmail())
            .orElseThrow(() -> {
                log.warn("Login gagal: Username atau email {} tidak ditemukan", request.getUsernameOrEmail());
                return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Username atau password salah!");
            });

        // Cocokkan password yang diinput dengan password terenkripsi di DB
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login gagal: Password salah untuk user {}", request.getUsernameOrEmail());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Username atau password salah!");
        }

        // LOGIKA FALLBACK DICEBEAR: Konsisten dengan fungsi updateUser, jika di database null, buatkan URL DiceBear otomatis
        String fotoProfilFinal = user.getImage() != null ? user.getImage()
                : "https://api.dicebear.com/7.x/initials/svg?seed=" + user.getUsername();

        log.info("User {} berhasil login", user.getUsername());

        // Generate Token JWT baru
        String token = jwtService.generateToken(user.getId());

        // Bangun data LoginResponse
        LoginResponse loginResponse = LoginResponse.builder()
            .id(String.valueOf(user.getId()))
            .username(user.getUsername())
            .fullname(user.getFullname())
            .email(user.getEmail())
            .image(fotoProfilFinal)
            .bio(user.getBio())
            .createdAt(user.getCreatedAt())
            .build();

        return ApiResponse.<LoginResponse>builder()
            .status(HttpStatus.OK.value())
            .message("User berhasil login")
            .data(loginResponse)
            .token(token)
            .build();
    }
}
