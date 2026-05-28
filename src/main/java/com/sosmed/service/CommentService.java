package com.sosmed.service;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sosmed.dto.ApiResponse;
import com.sosmed.dto.comment.CommentRequest;
import com.sosmed.dto.comment.CommentResponse;
import com.sosmed.dto.post.PostResponse;
import com.sosmed.repository.CommentRepository;
import com.sosmed.repository.PostRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final CloudinaryService cloudinaryService;

    @Transactional
    public ApiResponse<CommentResponse> createComment(Long userId, Long postId, CommentRequest request) {
        log.info("User ID {} mencoba membuat komentar di Post ID {}", userId, postId);

        // Validasi: Cek apakah Post dengan postId tersebut ada di database
        try {
            PostResponse post = postRepository.findPostResponseById(postId);
            if (post == null) {
                log.error("Postingan tidak ditemukan");
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Postingan tidak ditemukan!");
            }

        } catch (Exception e) {
            log.error("Post dengan ID {} tidak ditemukan atau terjadi error.", postId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Postingan tidak ditemukan!");
        }

        // Validasi: Konten tidak boleh kosong jika tidak mengirim gambar
        String content = request.getContent();
        if ((content == null || content.trim().isEmpty()) && (request.getImage() == null || request.getImage().isEmpty())) {
            log.error("Komentar tidak boleh kosong");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Komentar tidak boleh kosong!");
        }

        String imageUrl = null;
        String imageId = null;

        // Upload ke Cloudinary jika user menyertakan gambar di komentar
        if (request.getImage() != null && !request.getImage().isEmpty()) {
            log.info("Ditemukan file gambar komentar, mulai mengunggah ke Cloudinary...");
            Map uploadResult = cloudinaryService.uploadPostImage(request.getImage());
            imageUrl = (String) uploadResult.get("secure_url");
            imageId = (String) uploadResult.get("public_id");
        }

        // Simpan data komentar ke database
        Long commentId = commentRepository.save(userId, postId, content, imageUrl, imageId);
        if (commentId == null) {
            log.error("Gagal membuat komentar");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }

        // Update data tabel post (Increment comment_count)
        log.debug("Menaikkan jumlah comment_count untuk Post ID {}", postId);
        boolean isIncrementSuccess = postRepository.incrementCommentCount(postId);
        if (!isIncrementSuccess) {
            log.error("Gagal memperbarui total hitungan komentar pada postingan ID {}", postId);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }

        // Ambil data response lengkap yang digabung dengan user profile data
        CommentResponse commentResponse = commentRepository.findCommentResponseById(commentId);

        return ApiResponse.<CommentResponse>builder()
            .status(HttpStatus.OK.value())
            .message("Komentar berhasil ditambahkan!")
            .data(commentResponse)
            .build();
    }

    @Transactional
    public ApiResponse<CommentResponse> editComment(Long commentId, Long userId, CommentRequest request) {

        log.info("Memproses edit comment ID: {} untuk userId: {}", commentId, userId);

        // Cari dulu komentar di database
        CommentResponse existingComment;
        try {
            existingComment = commentRepository.findCommentResponseById(commentId);
        } catch (Exception e) {
            log.error("Komentar dengan ID {} tidak ditemukan", commentId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Komentar tidak ditemukan.");
        }

        // Validasi user yang mengedit adalah pemilik komentar tersebut
        if (!existingComment.getUser().getId().equals(userId)) {
            log.error("User {} mencoba mengedit komentar milik user {}", userId, existingComment.getUser().getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Anda tidak memiliki akses untuk mengedit komentar ini.");
        }

        String content = request.getContent();
        boolean hasContent = content != null && !content.trim().isEmpty();
        boolean hasNewImage = request.getImage() != null && !request.getImage().isEmpty();

        if (!hasContent && !hasNewImage && existingComment.getImage() == null) {
            log.error("Komentar tidak boleh kosong.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Komentar tidak boleh kosong. Harap isi teks atau unggah foto.");
        }

        // Ambil nilai lama sebagai default jika tidak ada perubahan gambar
        String imageUrl = existingComment.getImage();
        String imageId = existingComment.getImageId();

        // Logika penanganan gambar baru
        if (hasNewImage) {
            try {
                // Jika komentar lama punya gambar di Cloudinary, hapus dulu secara sinkron
                if (existingComment.getImageId() != null) {
                    log.info("Menghapus gambar lama di Cloudinary dengan Public ID: {}", existingComment.getImageId());

                    cloudinaryService.deletePostImage(existingComment.getImageId());
                }

                // Upload gambar baru
                Map uploadResult = cloudinaryService.uploadPostImage(request.getImage());
                imageUrl = (String) uploadResult.get("secure_url");
                imageId = (String) uploadResult.get("public_id");

                log.info("Sukses mengunggah gambar baru. URL: {}, Public ID: {}", imageUrl, imageId);

            } catch (Exception e) {
                log.error("Gagal memproses gambar saat edit komentar", e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
            }
        }

        // Update data ke database melalui repository
        boolean isUpdated = commentRepository.update(
            commentId,
            hasContent ? content.trim() : null,
            imageUrl,
            imageId);

        if (!isUpdated) {
            log.error("Gagal memperbarui data komentar di database.");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }

        // Ambil data yang sudah terupdate untuk dikembalikan ke client
        CommentResponse updateCommentResponse = commentRepository.findCommentResponseById(commentId);

        return ApiResponse.<CommentResponse>builder()
            .status(HttpStatus.OK.value())
            .message("Komentar berhasil diperbarui")
            .data(updateCommentResponse)
            .build();

    }

    /**
     * Menghapus komentar
     */
    @Transactional
    public ApiResponse<String> deleteComment(Long commentId, Long userId) {
        log.info("Memproses penghapusan komentar ID: {} oleh userId: {}", commentId, userId);

        // Cek keberadaan komentar
        CommentResponse commentResponse;
        try {
            commentResponse = commentRepository.findCommentResponseById(commentId);
        } catch (Exception e) {
            log.error("Komentar dengan ID {} tidak ditemukan", commentId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Komentar tidak ditemukan.");
        }

        // Validasi Kepemilikan
        if (!commentResponse.getUser().getId().equals(userId)) {
            log.error("User {} ilegal mencoba menghapus komentar milik user {}", userId, commentResponse.getUser().getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Anda tidak memiliki akses untuk menghapus komentar ini.");
        }

        // Hapus gambar di Cloudinary
        if (commentResponse.getImageId() != null && !commentResponse.getImageId().trim().isEmpty()) {
            try {
                log.info("Menghapus aset gambar di Cloudinary dengan ID: {}", commentResponse.getImageId());
                cloudinaryService.deletePostImage(commentResponse.getImageId());
            } catch (Exception e) {
                log.error("Gagal menghapus gambar di Cloudinary untuk comment ID {}", commentId, e);
            }
        }

        // Hapus comment
        boolean isDeleted = commentRepository.deleteById(commentId);
        if (!isDeleted) {
            log.error("Gagal menghapus data komentar di database. Comment ID: {} tidak ditemukan atau gagal dieksekusi.", commentId);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }

        // Update data tabel post (Decrement comment_count)
        // Ambil postId dari objek commentResponse
        Long postId = commentResponse.getPostId();

        log.info("Menurunkan jumlah comment_count untuk Post ID {}", postId);
        boolean isDecrementSuccess = postRepository.decrementCommentCount(postId);
        if (!isDecrementSuccess) {
            log.error("Gagal menurunkan hitungan komentar pada postingan ID {}", postId);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }

        return ApiResponse.<String>builder()
            .status(HttpStatus.OK.value())
            .message("Komentar berhasil dihapus")
            .data("Komentar berhasil dihapus")
            .build();
    }
    
}
