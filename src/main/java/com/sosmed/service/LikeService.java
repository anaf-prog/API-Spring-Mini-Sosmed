package com.sosmed.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sosmed.dto.ApiResponse;
import com.sosmed.dto.PagingResponse;
import com.sosmed.dto.like.LikeInfo;
import com.sosmed.dto.like.LikeResponse;
import com.sosmed.dto.like.UserLikeResponse;
import com.sosmed.repository.LikeRepository;
import com.sosmed.repository.PostRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LikeService {

    private final LikeRepository likeRepository;
    private final PostRepository postRepository;

    /**
     * Fitur Like dan Unlike.
     */
    @Transactional
    public ApiResponse<Object> toggleLike(Long userId, Long postId) {
        log.info("User ID {} mencoba melakukan toggle like pada Post ID {}", userId, postId);

        // Validasi apakah postingan yang akan di-like ada di database
        try {
            postRepository.findPostResponseById(postId);
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                log.error("Postingan tidak ditemukan! : {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Postingan tidak ditemukan!");
            }
            throw e;
        }

        // Cek status apakah user sudah pernah like post ini
        boolean isLiked = likeRepository.existsByUserIdAndPostId(userId, postId);

        if (isLiked) {
            // Jika sudah like -> lakukan unlike
            likeRepository.delete(userId, postId);
            postRepository.decrementLikeCount(postId);

            log.info("User ID {} berhasil unlike Post ID {}", userId, postId);
            
            return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Berhasil membatalkan menyukai postingan")
                .data("Sukses Unlike")
                .build();
        } else {
            // Jika belum like -> lakukan like
            likeRepository.save(userId, postId);
            postRepository.incrementLikeCount(postId);

            // Ambil info detail untuk response payload setelah berhasil insert
            LikeInfo info = likeRepository.findLikeInfo(userId, postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Gagal memproses data like"));

            String fallbackImage = info.getUserImage() != null ? info.getUserImage()
                : "https://api.dicebear.com/7.x/initials/svg?seed=" + info.getUsername();

            UserLikeResponse userLikeResponse = UserLikeResponse.builder()
                .id(info.getUserId())
                .fullname(info.getFullname())
                .username(info.getUsername())
                .image(fallbackImage)
                .build();

            LikeResponse likeResponse = LikeResponse.builder()
                .postId(info.getPostId())
                .createdAt(info.getCreatedAt())
                .updatedAt(info.getUpdatedAt())
                .user(userLikeResponse)
                .build();

            log.info("User ID {} berhasil menyukai Post ID {}", userId, postId);

            return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Berhasil menyukai postingan")
                .data(likeResponse)
                .build();
        }
    }

    /**
     * Mengambil semua daftar user yang me-like suatu postingan dengan Paginasi.
     */
    @Transactional(readOnly = true)
    public ApiResponse<List<LikeResponse>> getPostLikes(Long postId, int page, int size) {
        log.info("Mengambil daftar likes untuk Post ID {} - Halaman: {}, Ukuran: {}", postId, page, size);

        // Validasi postingan
        try {
            postRepository.findPostResponseById(postId);
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Postingan tidak ditemukan!");
            }
            throw e;
        }

        // Hitung total data
        long totalElements = likeRepository.countLikesByPostId(postId);

        // Hitung total halaman
        int totalPages = (int) Math.ceil((double) totalElements / size);

        // Hitung offset
        long offset = (long) page * size;

        // Ambil data dari database menggunakan limit dan offset
        List<LikeInfo> likeInfoList = likeRepository.findLikesByPostIdWithPaging(postId, size, offset);

        List<LikeResponse> likeResponses = likeInfoList.stream().map(info -> {
            String fallbackImage = info.getUserImage() != null ? info.getUserImage()
                : "https://api.dicebear.com/7.x/initials/svg?seed=" + info.getUsername();

            UserLikeResponse userDetail = UserLikeResponse.builder()
                .id(info.getUserId())
                .fullname(info.getFullname())
                .username(info.getUsername())
                .image(fallbackImage)
                .build();

            return LikeResponse.builder()
                .postId(info.getPostId())
                .createdAt(info.getCreatedAt())
                .updatedAt(info.getUpdatedAt())
                .user(userDetail)
                .build();
        }).collect(Collectors.toList());

        PagingResponse paging = PagingResponse.builder()
            .currentPage(page)
            .totalPages(totalPages)
            .size(size)
            .totalElements(totalElements)
            .build();

        return ApiResponse.<List<LikeResponse>>builder()
            .status(HttpStatus.OK.value())
            .message("Berhasil mengambil daftar likes postingan")
            .data(likeResponses)
            .paging(paging)
            .build();
    }
    
}
