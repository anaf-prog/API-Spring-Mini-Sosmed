package com.sosmed.service;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sosmed.dto.ApiResponse;
import com.sosmed.dto.PagingResponse;
import com.sosmed.dto.user.UserResponse;
import com.sosmed.dto.user.UserUpdateRequest;
import com.sosmed.model.User;
import com.sosmed.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    public ApiResponse<UserResponse> getCurrentUser(Long userId) {

        User user = userRepository.findById(userId)
            .orElseThrow(() -> {
                log.error("Gagal memuat profile: User dengan ID {} tidak ditemukan", userId);
                return new ResponseStatusException(HttpStatus.NOT_FOUND, "User tidak ditemukan!");
            });

        // LOGIKA FALLBACK DICEBEAR: Konsisten dengan fungsi updateUser, jika di database null, buatkan URL DiceBear otomatis
        String fotoProfilFinal = user.getImage() != null ? user.getImage()
                : "https://api.dicebear.com/7.x/initials/svg?seed=" + user.getUsername();    

        UserResponse userResponse = UserResponse.builder()
            .id(user.getId())
            .fullname(user.getFullname())
            .username(user.getUsername())
            .email(user.getEmail())
            .image(fotoProfilFinal)
            .bio(user.getBio())
            .followerCount(user.getFollowerCount())
            .followingCount(user.getFollowingCount())
            .postCount(user.getPostCount())
            .createdAt(user.getCreatedAt())
            .build();
            
        return ApiResponse.<UserResponse>builder()
            .status(HttpStatus.OK.value())
            .message("Data user saat ini")
            .data(userResponse)
            .build();    
    }

    /**
     * Fungsi utama untuk mengupdate profile user secara parsial (PATCH) dan
     * opsional.
     */
    @Transactional
    public ApiResponse<UserResponse> updateUser(Long userId, UserUpdateRequest request) {
        // 1. Cek apakah user yang mau diupdate ada di database
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User tidak ditemukan!"));

        // Helper untuk menormalisasi string kosong ("") atau spasi menjadi null
        String fullnameInput = (request.getFullname() != null && !request.getFullname().trim().isEmpty()) ? request.getFullname().trim() : null;
        String usernameInput = (request.getUsername() != null && !request.getUsername().trim().isEmpty()) ? request.getUsername().trim() : null;
        String emailInput = (request.getEmail() != null && !request.getEmail().trim().isEmpty()) ? request.getEmail().trim() : null;
        String passwordInput = (request.getPassword() != null && !request.getPassword().trim().isEmpty()) ? request.getPassword() : null;
        String bioInput = (request.getBio() != null && !request.getBio().trim().isEmpty()) ? request.getBio().trim() : null;

        // 2. Validasi keunikan jika user berniat mengganti username
        if (request.getUsername() != null && !request.getUsername().equals(currentUser.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username sudah digunakan!");
            }
        }

        // 3. Validasi keunikan jika user berniat mengganti email
        if (request.getEmail() != null && !request.getEmail().equals(currentUser.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email sudah digunakan!");
            }
        }

        // 4. Petakan data DTO ke dalam entity khusus penampung nilai teks PATCH
        User patchData = User.builder()
            .fullname(fullnameInput)
            .username(usernameInput)
            .email(emailInput)
            .password(passwordInput) // Pastikan diencode bcrypt terlebih dahulu jika diimplementasikan
            .bio(bioInput)
            .build();

        // 5. Eksekusi pembaruan data 
        userRepository.updateUser(userId, patchData);

        // 6. Tangani pengelolaan gambar Cloudinary secara non-blocking jika ada file baru yang masuk
        if (request.getImage() != null && !request.getImage().isEmpty()) {

            try {
                // AMBIL BYTES DI SINI (Thread Utama), sebelum dilempar ke Virtual Thread
                byte[] gambarBytes = request.getImage().getBytes();
                String oldImageId = currentUser.getImageId();

                // Kirim data yang sudah aman berupa byte[] ke CloudinaryService
                cloudinaryService.uploadAndHandleOldImageAsync(userId, gambarBytes, oldImageId);

                log.info("Berhasil menjadwalkan upload gambar untuk user ID: {}", userId);

            } catch (IOException e) {
                log.error("Gagal membaca file gambar dari request untuk user ID {}: {}", userId, e.getMessage());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File gambar rusak");
            }
        }

        // 7. Ambil data terbaru dari database untuk dikembalikan sebagai respons
        User updatedUser = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Gagal update"));

        // LOGIKA FALLBACK DICEBEAR: Jika kolom image di database null, kita buatkan URL inisial dinamis berdasarkan username terbaru
        String fotoProfilFinal = updatedUser.getImage() != null ? updatedUser.getImage()
                : "https://api.dicebear.com/7.x/initials/svg?seed=" + updatedUser.getUsername();        

        UserResponse userResponse = UserResponse.builder()
            .id(updatedUser.getId())
            .fullname(updatedUser.getFullname())
            .username(updatedUser.getUsername())
            .email(updatedUser.getEmail())
            .image(fotoProfilFinal) 
            .bio(updatedUser.getBio())
            .followerCount(updatedUser.getFollowerCount())
            .followingCount(updatedUser.getFollowingCount())
            .postCount(updatedUser.getPostCount())
            .createdAt(updatedUser.getCreatedAt())
            .updatedAt(updatedUser.getUpdatedAt())
            .build();

        return ApiResponse.<UserResponse>builder()
            .status(HttpStatus.OK.value())
            .message("Berhasil update.")
            .data(userResponse)
            .build();
    }

    public ApiResponse<List<UserResponse>> getAllUsers(int page, int size) {

        // Hitung total elemen
        long totalElements = userRepository.countAllUsers();
        
        // Hitung total halaman
        int totalPages = (int) Math.ceil((double) totalElements / size);
        
        // Cari offset berdasarkan halaman saat ini (Halaman dimulai dari indeks 0)
        int offset = page * size;
        
        // Ambil data user dari repository
        List<User> users = userRepository.findAllWithPagination(size, offset);
        
        // Mapping dari List<User> ke List<UserResponse>
        List<UserResponse> userResponses = users.stream().map(user -> UserResponse.builder()
            .id(user.getId())
            .fullname(user.getFullname())
            .username(user.getUsername())
            .email(user.getEmail())
            .image(user.getImage())
            .bio(user.getBio())
            .followerCount(user.getFollowerCount())
            .followingCount(user.getFollowingCount())
            .postCount(user.getPostCount())
            .createdAt(user.getCreatedAt())
            .build()
        ).collect(Collectors.toList());

        // Bungkus informasi halaman ke dalam PagingResponse
        PagingResponse paging = PagingResponse.builder()
            .currentPage(page)
            .totalPages(totalPages)
            .size(size)
            .totalElements(totalElements)
            .build();

        return ApiResponse.<List<UserResponse>>builder()
            .status(HttpStatus.OK.value())
            .message("Berhasil tampilkan semua data user")
            .paging(paging)
            .data(userResponses)
            .build();
    }
    
}
