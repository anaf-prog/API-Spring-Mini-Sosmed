package com.sosmed.Auth;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.sosmed.model.User;
import com.sosmed.repository.UserRepository;

@JdbcTest(properties = {
    // Alihkan koneksi langsung ke database kosong khusus test 'sosmed_test'
    "spring.datasource.url=jdbc:postgresql://localhost:5432/sosmed_test",
    "spring.datasource.username=postgres",
    "spring.datasource.password=anaf170996"
})
@Import(UserRepository.class) // 2. Memasukkan UserRepository asli ke konteks test
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // 3. Gunakan database Postgres 'sosmed_test'
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        // Membersihkan tabel sebelum setiap pengujian dijalankan untuk menghindari konflik data
        jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
    }

    @Test
    @DisplayName("Simpan dan Cari User - Harus berhasil menyimpan ke DB dan memanggilnya kembali")
    void createAndFindUser_Success() {
        // Arrange
        User user = User.builder()
            .fullname("Andi Wijaya")
            .username("andiw")
            .email("andi@mail.com")
            .password("EncryptedPassword123")
            .bio("Halo dunia!")
            .image("https://api.dicebear.com/7.x/initials/svg?seed=andiw")
            .imageId("img_andi")
            .build();

        // Act: Jalankan fungsi query insert database
        User savedUser = userRepository.creatUser(user);

        // Assert: Pastikan data sukses masuk dan ID auto-increment dari DB terisi
        assertNotNull(savedUser.getId());
        assertEquals("andiw", savedUser.getUsername());
        System.out.println("User berhasil di-insert ke DB dengan ID: " + savedUser.getId());

        // Act 2: Coba cari user tersebut berdasarkan username/email
        Optional<User> foundUser = userRepository.findByUsernameOrEmail("andiw");

        // Assert 2: Pastikan data yang ditarik dari DB sama persis
        assertTrue(foundUser.isPresent());
        assertEquals("Andi Wijaya", foundUser.get().getFullname());
        System.out.println("User ditemukan di DB dengan Nama: " + foundUser.get().getFullname());
    }

    @Test
    @DisplayName("Check Keberadaan Email/Username - Harus mengembalikan true jika data ada")
    void existsByEmailOrUsername_Success() {
        // Arrange
        User user = User.builder()
            .fullname("Budi Santoso")
            .username("budis")
            .email("budi@mail.com")
            .password("Encrypted")
            .build();
        userRepository.creatUser(user);

        // Act & Assert (Cek Email)
        boolean emailExists = userRepository.existsByEmail("budi@mail.com");
        boolean emailNotExists = userRepository.existsByEmail("nonexistent@mail.com");
        assertTrue(emailExists);
        assertFalse(emailNotExists);

        // Act & Assert (Cek Username)
        boolean usernameExists = userRepository.existsByUsername("budis");
        boolean usernameNotExists = userRepository.existsByUsername("nonexistent");
        assertTrue(usernameExists);
        assertFalse(usernameNotExists);

        System.out.println("Keberadaan email budi@mail.com: " + emailExists);
        System.out.println("Keberadaan username budis: " + usernameExists);
    }

    @Test
    @DisplayName("Update User Secara Dinamis - Hanya kolom non-null yang diubah")
    void updateUser_Success() {
        // Arrange
        User user = User.builder()
            .fullname("Cici Rahma")
            .username("cicir")
            .email("cici@mail.com")
            .password("Encrypted")
            .bio("Bio lama")
            .build();
        User savedUser = userRepository.creatUser(user);

        // Siapkan objek user yang HANYA berisi kolom yang ingin kita ubah saja
        User updateData = User.builder()
            .fullname("Cici Rahmawati") // Berubah
            .bio("Bio baru yang diperbarui") // Berubah
            .build(); // Kolom username, email, password dibiarkan null

        // --- CETAK DATA SEBELUM UPDATE ---
        System.out.println("=== DATA SEBELUM UPDATE ===");
        System.out.println("ID User           : " + savedUser.getId());
        System.out.println("Nama Sebelum      : " + savedUser.getFullname());
        System.out.println("Bio Sebelum       : " + savedUser.getBio());
        System.out.println("Username Sebelum  : " + savedUser.getUsername());
        System.out.println("Email Sebelum     : " + savedUser.getEmail());
        System.out.println("===========================\n");

        // Act
        userRepository.updateUser(savedUser.getId(), updateData);

        // Assert: Pastikan hanya data yang diisi yang berubah, kolom null tidak menimpa
        // data lama
        Optional<User> updatedUserOpt = userRepository.findById(savedUser.getId());
        assertTrue(updatedUserOpt.isPresent());
        User updatedUser = updatedUserOpt.get();

        assertEquals("Cici Rahmawati", updatedUser.getFullname()); // Berubah
        assertEquals("Bio baru yang diperbarui", updatedUser.getBio()); // Berubah
        assertEquals("cicir", updatedUser.getUsername()); // Tetap, tidak menjadi null
        assertEquals("cici@mail.com", updatedUser.getEmail()); // Tetap, tidak menjadi null

        // --- CETAK DATA SETELAH UPDATE ---
        System.out.println("=== DATA SETELAH UPDATE ===");
        System.out.println("Nama Setelah      : " + updatedUser.getFullname());
        System.out.println("Bio Setelah       : " + updatedUser.getBio());
        System.out.println("Username Setelah  : " + updatedUser.getUsername()); // Memastikan tidak berubah jadi null
        System.out.println("Email Setelah     : " + updatedUser.getEmail()); // Memastikan tidak berubah jadi null
        System.out.println("===========================");
    }

    @Test
    @DisplayName("Increment dan Decrement Post Count - Mengubah counter jumlah post")
    void incrementAndDecrementPostCount_Success() {
        // Arrange
        User user = User.builder()
            .fullname("Dedi Kurnia")
            .username("dedik")
            .email("dedi@mail.com")
            .password("Encrypted")
            .build();
        User savedUser = userRepository.creatUser(user);
        assertEquals(0, savedUser.getPostCount()); // Default post_count di DB adalah 0

        // Act: Naikkan 2 kali
        userRepository.incrementPostCount(savedUser.getId());
        userRepository.incrementPostCount(savedUser.getId());

        // Assert
        User userAfterIncrement = userRepository.findById(savedUser.getId()).orElseThrow();
        assertEquals(2, userAfterIncrement.getPostCount());
        System.out.println("Jumlah post setelah di-increment 2x: " + userAfterIncrement.getPostCount());

        // Act: Turunkan 1 kali
        userRepository.decrementPostCount(savedUser.getId());

        // Assert
        User userAfterDecrement = userRepository.findById(savedUser.getId()).orElseThrow();
        assertEquals(1, userAfterDecrement.getPostCount());
        System.out.println("Jumlah post setelah di-decrement 1x: " + userAfterDecrement.getPostCount());
    }
    
}
