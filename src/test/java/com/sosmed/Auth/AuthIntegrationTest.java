package com.sosmed.Auth;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sosmed.dto.auth.LoginRequest;
import com.sosmed.dto.auth.RegisterRequest;
import com.sosmed.model.User;
import com.sosmed.repository.UserRepository;

@SpringBootTest(properties = {
    // 1. Alihkan koneksi dari database utama 'sosmed' ke database kosong khusus test 'sosmed_test'
    "spring.datasource.url=jdbc:postgresql://localhost:5432/sosmed_test",

    // 2. Buat Spring untuk selalu membaca file schema.sql
    "spring.sql.init.mode=always"
})
@AutoConfigureMockMvc
@Transactional
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Register Berhasil")
    void register_UserIntegration_Success() throws Exception {
        // 1. Siapkan data request tiruan
        RegisterRequest request = RegisterRequest.builder()
            .email("annaf@mail.com")
            .username("testanaf")
            .fullname("Anaf Test Integration")
            .password("Annaf12345@")
            .build();

        // 2. Jalankan HTTP Request ke Controller dan ambil MvcResult-nya
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isCreated())
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("User berhasil registrasi"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.token").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.username").value("testanaf"))
            .andReturn(); // Mengambil object kembalian untuk diekstrak string bodynya

        String rawJson = result.getResponse().getContentAsString();
        System.out.println("\n================================================");
        System.out.println("[TEST INTEGRATION] REGISTER BERHASIL");
        System.out.println("================================================");
        if (rawJson != null && !rawJson.isBlank()) {
            Object jsonObject = objectMapper.readValue(rawJson, Object.class);
            String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
            System.out.println("RESPONSE BODY FROM SERVER:");
            System.out.println(prettyJson);
        }
        System.out.println("================================================\n");

        // 3. Verifikasi ke Database menggunakan method JDBC
        Optional<User> userInDatabase = userRepository.findByUsernameOrEmail("testanaf");
        Assertions.assertTrue(userInDatabase.isPresent());
        Assertions.assertEquals("Anaf Test Integration", userInDatabase.get().getFullname());
    }

    @Test
    @DisplayName("Register Gagal - Password Tidak Sesuai Regex")
    void register_UserIntegration_FailedPasswordRegex() throws Exception {
        // 1. Arrange: Siapkan data request dengan password tidak sesuai regex (tanpa huruf besar, angka, dan karakter khusus)
        RegisterRequest request = RegisterRequest.builder()
            .email("anaf_gagal@mail.com")
            .username("anafgagal")
            .fullname("Anaf Gagal Password")
            .password("password") // Sengaja dibuat salah total
            .build();

        // 2. Act & Assert: Hit API register dan pastikan status kembalian adalah 400 Bad Request
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest()) // Validasi bahwa status harus 400
            .andReturn();

        // 3. Ekstrak pesan error validasi regex password untuk dicetak di log console
        String prettyJsonRequest = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        Exception resolvedException = result.getResolvedException();
        String errorMessage = "No Exception";

        if (resolvedException instanceof MethodArgumentNotValidException ex) {
            FieldError fieldError = ex.getBindingResult().getFieldError("password");
            if (fieldError != null) {
                errorMessage = fieldError.getDefaultMessage();
            }
        }

        System.out.println("\n================================================");
        System.out.println("[TEST INTEGRATION] REGISTER GAGAL - PASSWORD TIDAK SESUAI SPESIFIKASI");
        System.out.println("================================================");
        System.out.println("DATA INPUT REQUEST:");
        System.out.println(prettyJsonRequest);
        System.out.println("------------------------------------------------");
        System.out.println("HASIL EXCEPTION VALIDASI:");
        System.out.println(errorMessage);
        System.out.println("================================================\n");

        // 4. Pengaman Tambahan: Pastikan data user yang gagal ini tidak tersimpan di database
        Optional<User> userInDatabase = userRepository.findByUsernameOrEmail("anafgagal");
        Assertions.assertTrue(userInDatabase.isEmpty(), "User seharusnya tidak tersimpan di DB karena validasi gagal");
    }

    @Test
    @DisplayName("Register Gagal - Username kosong")
    void register_UserIntegration_FailedUsernameNull() throws Exception {
        // 1. Arrange: Siapkan data request dengan username kosong
        RegisterRequest request = RegisterRequest.builder()
            .email("anaf_gagal@mail.com")
            .username(null) // Sengaja dibuat null
            .fullname("Anaf Gagal Password")
            .password("Password12345@") 
            .build();

        // 2. Act & Assert: Hit API register dan pastikan status kembalian adalah 400
        // Bad Request
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest()) // Validasi bahwa status harus 400
            .andReturn();

        // 3. Ekstrak pesan error validasi username untuk dicetak di log console
        String prettyJsonRequest = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        Exception resolvedException = result.getResolvedException();
        String errorMessage = "No Exception";

        if (resolvedException instanceof MethodArgumentNotValidException ex) {
            FieldError fieldError = ex.getBindingResult().getFieldError("username");
            if (fieldError != null) {
                errorMessage = fieldError.getDefaultMessage();
            }
        }

        System.out.println("\n================================================");
        System.out.println("[TEST INTEGRATION] REGISTER GAGAL - USERNAME KOSONG");
        System.out.println("================================================");
        System.out.println("DATA INPUT REQUEST:");
        System.out.println(prettyJsonRequest);
        System.out.println("------------------------------------------------");
        System.out.println("HASIL EXCEPTION VALIDASI:");
        System.out.println(errorMessage);
        System.out.println("================================================\n");

        // 4. Pengaman Tambahan: Pastikan data user yang gagal ini tidak tersimpan di database
        Optional<User> userInDatabase = userRepository.findByUsernameOrEmail("anafgagal");
        Assertions.assertTrue(userInDatabase.isEmpty(), "User seharusnya tidak tersimpan di DB karena validasi gagal");
    }

    @Test
    @DisplayName("Register Gagal - Email Sudah Terdaftar")
    void register_UserIntegration_FailedEmailExists() throws Exception {
        // 1. Arrange: Masukkan data user pertama secara langsung ke DB lewat repository sebagai data master
        User existingUser = User.builder()
            .email("anaf_terdaftar@mail.com")
            .username("anaf_lama")
            .fullname("Anaf Lama")
            .password("Password123@")
            .build();
        userRepository.creatUser(existingUser);

        // 2. Siapkan data request pendaftaran baru yang menggunakan Email sama, tapi username beda
        RegisterRequest request = RegisterRequest.builder()
            .email("anaf_terdaftar@mail.com") // Email sama persis
            .username("anaf_baru")
            .fullname("Anaf Baru")
            .password("Password123@")
            .build();

        // 3. Act & Assert: Hit API register, pastikan mengembalikan status 400 Bad
        // Request
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andReturn();

        // 4. Ambil teks exception kustom langsung dari ResponseStatusException
        String prettyJsonRequest = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        Exception resolvedException = result.getResolvedException();
        String errorMessage = (resolvedException != null) ? resolvedException.getMessage() : "No Exception";

        System.out.println("\n================================================");
        System.out.println("[TEST INTEGRATION] REGISTER GAGAL - EMAIL SUDAH TERDAFTAR");
        System.out.println("================================================");
        System.out.println("DATA INPUT REQUEST:");
        System.out.println(prettyJsonRequest);
        System.out.println("------------------------------------------------");
        System.out.println("HASIL EXCEPTION SERVER:");
        System.out.println(errorMessage);
        System.out.println("================================================\n");

        // 5. Pastikan user dengan username "anaf_baru" tidak masuk ke database
        Optional<User> userBaruInDatabase = userRepository.findByUsernameOrEmail("anaf_baru");
        Assertions.assertTrue(userBaruInDatabase.isEmpty(), "User baru harusnya gagal tersimpan karena email duplikat");
    }

    @Test
    @DisplayName("Register Gagal - Username Sudah Terdaftar")
    void register_UserIntegration_FailedUsernameExists() throws Exception {
        // 1. Arrange: Masukkan data user pertama secara langsung ke DB lewat repository sebagai data master
        User existingUser = User.builder()
            .email("anaf_terdaftar@mail.com")
            .username("anaf_lama")
            .fullname("Anaf Lama")
            .password("Password123@")
            .build();
        userRepository.creatUser(existingUser);

        // 2. Siapkan data request pendaftaran baru yang menggunakan Username sama
        RegisterRequest request = RegisterRequest.builder()
            .email("anaf_belumterdaftar@mail.com") 
            .username("anaf_lama") // Username sama persis
            .fullname("Anaf Baru")
            .password("Password123@")
            .build();

        // 3. Act & Assert: Hit API register, pastikan mengembalikan status 400 Bad Request
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andReturn();

        // 4. Ambil teks exception kustom langsung dari ResponseStatusException
        String prettyJsonRequest = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        Exception resolvedException = result.getResolvedException();
        String errorMessage = (resolvedException != null) ? resolvedException.getMessage() : "No Exception";

        System.out.println("\n================================================");
        System.out.println("[TEST INTEGRATION] REGISTER GAGAL - USERNAME SUDAH TERDAFTAR");
        System.out.println("================================================");
        System.out.println("DATA INPUT REQUEST:");
        System.out.println(prettyJsonRequest);
        System.out.println("------------------------------------------------");
        System.out.println("HASIL EXCEPTION SERVER:");
        System.out.println(errorMessage);
        System.out.println("================================================\n");

        // 5. Pastikan user dengan email "anaf_belumterdaftar@mail.com" tidak masuk ke database
        Optional<User> userBaruInDatabase = userRepository.findByUsernameOrEmail("anaf_belumterdaftar@mail.com");
        Assertions.assertTrue(userBaruInDatabase.isEmpty(), "User baru harusnya gagal tersimpan karena email duplikat");
    }

    @Test
    @DisplayName("Login Sukses - Menggunakan Username atau Email Valid")
    void login_UserIntegration_Success() throws Exception {
        // 1. Arrange: Buat password mentah asli dan daftarkan user baru langsung ke database dengan password terenkripsi
        String rawPassword = "Password12345@";
        User existingUser = User.builder()
            .email("anaf_login@mail.com")
            .username("anaflogin")
            .fullname("Anaf Sukses Login")
            .password(passwordEncoder.encode(rawPassword)) // Enkripsi password menggunakan bCrypt bawaan Spring
            .build();
        userRepository.creatUser(existingUser);

        // 2. Siapkan data request login (Bisa menggunakan username ataupun email)
        LoginRequest request = LoginRequest.builder()
            .usernameOrEmail("anaflogin") // Bisa dicoba juga diganti pakai "anaf_login@mail.com"
            .password(rawPassword)
            .build();

        // 3. Act & Assert: Hit API login, pastikan mengembalikan status 200 OK beserta Token JWT dan Data Profil
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isOk()) // Memastikan status HTTP 200 OK
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(200))
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("User berhasil login"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.token").exists()) // Token JWT wajib terbentuk
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.username").value("anaflogin"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.email").value("anaf_login@mail.com"))
            .andReturn();

        // 4. Ekstrak dan cetak response JSON secara rapi ke console log
        String rawJson = result.getResponse().getContentAsString();
        System.out.println("\n================================================");
        System.out.println("[TEST INTEGRATION] LOGIN SUKSES");
        System.out.println("================================================");
        if (rawJson != null && !rawJson.isBlank()) {
            Object jsonObject = objectMapper.readValue(rawJson, Object.class);
            String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
            System.out.println("RESPONSE BODY FROM SERVER:");
            System.out.println(prettyJson);
        }
        System.out.println("================================================\n");
    }

    @Test
    @DisplayName("Login Gagal - Username Tidak Ditemukan")
    void login_UserIntegration_FailedUsernameNotFound() throws Exception {
        // 1. Arrange: Siapkan data login dengan username yang memang dipastikan tidak ada di DB
        LoginRequest request = LoginRequest.builder()
            .usernameOrEmail("username_tidak_terdaftar")
            .password("Password123@")
            .build();

        // 2. Act & Assert: Hit API login, pastikan mengembalikan status 401 Unauthorized
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isUnauthorized()) // Sesuai dengan status 401
            .andReturn();

        // 3. Ekstrak Exception dan cetak ke console
        String prettyJsonRequest = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        Exception resolvedException = result.getResolvedException();
        String errorMessage = (resolvedException != null) ? resolvedException.getMessage() : "No Exception";

        System.out.println("\n================================================");
        System.out.println("[TEST INTEGRATION] LOGIN GAGAL - USERNAME TIDAK DITEMUKAN");
        System.out.println("================================================");
        System.out.println("DATA INPUT REQUEST:");
        System.out.println(prettyJsonRequest);
        System.out.println("------------------------------------------------");
        System.out.println("HASIL EXCEPTION SERVER:");
        System.out.println(errorMessage);
        System.out.println("================================================\n");
    }

    @Test
    @DisplayName("Login Gagal - Password Salah")
    void login_UserIntegration_FailedPasswordIncorrect() throws Exception {
        // 1. Arrange: Masukkan data master user dengan password asli "Password123@" yang terenkripsi
        User existingUser = User.builder()
            .email("anaf_salah_pass@mail.com")
            .username("anafsalahpass")
            .fullname("Anaf Salah Password")
            .password(passwordEncoder.encode("Password123@"))
            .build();
        userRepository.creatUser(existingUser);

        // 2. Siapkan data request login dengan username benar, tapi password sengaja dibuat salah
        LoginRequest request = LoginRequest.builder()
            .usernameOrEmail("anafsalahpass")
            .password("PasswordSalahBung!") // Password tidak sesuai dengan database
            .build();

        // 3. Act & Assert: Hit API login, pastikan mengembalikan status 401 Unauthorized
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
            .andDo(MockMvcResultHandlers.print())
            .andExpect(MockMvcResultMatchers.status().isUnauthorized()) // Status wajib 401
            .andReturn();

        // 4. Ekstrak Exception dan cetak ke console
        String prettyJsonRequest = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
        Exception resolvedException = result.getResolvedException();
        String errorMessage = (resolvedException != null) ? resolvedException.getMessage() : "No Exception";

        System.out.println("\n================================================");
        System.out.println("[TEST INTEGRATION] LOGIN GAGAL - PASSWORD SALAH");
        System.out.println("================================================");
        System.out.println("DATA INPUT REQUEST:");
        System.out.println(prettyJsonRequest);
        System.out.println("------------------------------------------------");
        System.out.println("HASIL EXCEPTION SERVER:");
        System.out.println(errorMessage);
        System.out.println("================================================\n");
    }
    
}
