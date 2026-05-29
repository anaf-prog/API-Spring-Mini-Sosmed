package com.sosmed.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sosmed.dto.ApiResponse;
import com.sosmed.dto.PagingResponse;
import com.sosmed.dto.bookmark.BookmarkInfo;
import com.sosmed.dto.bookmark.BookmarkResponse;
import com.sosmed.dto.bookmark.UserBookmarkResponse;
import com.sosmed.repository.BookmarkRepository;
import com.sosmed.repository.PostRepository;
import com.sosmed.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    /**
     * Fitur Bookmark dan Unbookmark.
     */
    @Transactional
    public ApiResponse<Object> toggleBookmark(Long userId, Long postId) {
        log.info("User ID {} mencoba melakukan toggle bookmark pada Post ID {}", userId, postId);

        // Validasi apakah postingan yang akan di-bookmark ada di database
        try {
            postRepository.findPostResponseById(postId);
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                log.error("Postingan tidak ditemukan! : {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Postingan tidak ditemukan!");
            }
            throw e;
        }

        // Cek status apakah user sudah pernah bookmark post ini
        boolean isBookmarked = bookmarkRepository.existByUserIdAndPostId(userId, postId);

        if (isBookmarked) {
            // Jika sudah bookmark -> lakukan unbookmark
            bookmarkRepository.delete(userId, postId);
            postRepository.decrementBookmarkCount(postId);
            userRepository.decrementBookmarkCount(userId);

            log.info("User ID {} berhasil unbookmark Post ID {}", userId, postId);
            
            return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Berhasil membatalkan membookmark postingan")
                .data("Sukses Unbookmark")
                .build();
        } else {
            // Jika belum membookmark -> lakukan bookmaek
            bookmarkRepository.save(userId, postId);
            postRepository.incrementBookmarkCount(postId);
            userRepository.incrementBookmarkCount(userId);

            // Ambil info detail untuk response payload setelah berhasil insert
            BookmarkInfo info = bookmarkRepository.findBookmarkInfo(userId, postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Gagal memproses data bookmark"));

            String fallbackImage = info.getUserImage() != null ? info.getUserImage()
                : "https://api.dicebear.com/7.x/initials/svg?seed=" + info.getUsername();

            UserBookmarkResponse userBookmarkResponse = UserBookmarkResponse.builder()
                .id(info.getUserId())
                .fullname(info.getFullname())
                .username(info.getUsername())
                .image(fallbackImage)
                .build();

            BookmarkResponse bookmarkResponse = BookmarkResponse.builder()
                .postId(info.getPostId())
                .createdAt(info.getCreatedAt())
                .updatedAt(info.getUpdatedAt())
                .user(userBookmarkResponse)
                .build();

            log.info("User ID {} berhasil membookmark Post ID {}", userId, postId);

            return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .message("Berhasil membookmark postingan")
                .data(bookmarkResponse)
                .build();
        }
    }

    /**
     * Mengambil semua daftar user yang membookmark suatu postingan dengan Paginasi.
     */
    @Transactional(readOnly = true)
    public ApiResponse<List<BookmarkResponse>> getPostBookmarks(Long postId, int page, int size) {
        log.info("Mengambil daftar bookmarks untuk Post ID {} - Halaman: {}, Ukuran: {}", postId, page, size);

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
        long totalElements = bookmarkRepository.countBookmarksByPostId(postId);

        // Hitung total halaman
        int totalPages = (int) Math.ceil((double) totalElements / size);

        // Hitung offset
        long offset = (long) page * size;

        // Ambil data dari database menggunakan limit dan offset
        List<BookmarkInfo> bookmarkInfoList = bookmarkRepository.findBookmarkByPostIdWithPaging(postId, size, offset);

        List<BookmarkResponse> bookmarkResponses = bookmarkInfoList.stream().map(info -> {
            String fallbackImage = info.getUserImage() != null ? info.getUserImage()
                : "https://api.dicebear.com/7.x/initials/svg?seed=" + info.getUsername();

            UserBookmarkResponse userDetail = UserBookmarkResponse.builder()
                .id(info.getUserId())
                .fullname(info.getFullname())
                .username(info.getUsername())
                .image(fallbackImage)
                .build();

            return BookmarkResponse.builder()
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

        return ApiResponse.<List<BookmarkResponse>>builder()
            .status(HttpStatus.OK.value())
            .message("Berhasil mengambil daftar bookmarks postingan")
            .data(bookmarkResponses)
            .paging(paging)
            .build();
    }
    
}
