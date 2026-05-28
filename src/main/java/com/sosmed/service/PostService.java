package com.sosmed.service;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sosmed.dto.ApiResponse;
import com.sosmed.dto.PagingResponse;
import com.sosmed.dto.post.PostCommentResponse;
import com.sosmed.dto.post.PostDetailResponse;
import com.sosmed.dto.post.PostRequest;
import com.sosmed.dto.post.PostResponse;
import com.sosmed.repository.PostRepository;
import com.sosmed.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;

    @Transactional
    public ApiResponse<PostResponse> createPost(Long userId, PostRequest request) {
        log.info("Memproses pembuatan postingan untuk userId: {}", userId);

        String caption = request.getCaption();
        boolean hasCaption = caption != null && !caption.trim().isEmpty();
        boolean hasImage = request.getImage() != null && !request.getImage().isEmpty();

        if (!hasCaption && !hasImage) {
            log.error("Postingan tidak boleh kosong. Harap isi teks atau unggah foto.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Postingan tidak boleh kosong. Harap isi teks atau unggah foto.");
        }

        String imageUrl = null;
        String imageId = null;

        if (hasImage) {
            try {
                Map uploadResult = cloudinaryService.uploadPostImage(request.getImage());
                imageUrl = (String) uploadResult.get("secure_url");
                imageId = (String) uploadResult.get("public_id");

                log.info("Sukses mengunggah gambar post. URL: {}, Public ID: {}", imageUrl, imageId);

            } catch (Exception e) {
                log.error("Gagal mengunggah gambar post ke Cloudinary", e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Gagal memproses gambar postingan, silakan coba lagi.");
            }
        }

        // Simpan data ke database melalui repository
        Long generatedPostId = postRepository.save(
            userId,
            hasCaption ? caption.trim() : null,
            imageUrl,
            imageId);

        if (generatedPostId == null) {
            log.error("Gagal membuat postingan.");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Gagal membuat postingan.");
        }

        // Update data tabel user (Increment post_count)
        log.debug("Menaikkan jumlah post_count untuk User ID {}", userId);
        boolean isIncrementSuccess = userRepository.incrementPostCount(userId);
        if (!isIncrementSuccess) {
            log.error("Gagal memperbarui total postingan pada User ID {}", userId);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }

        // Ambil data utuh hasil insert (termasuk join dengan data user) 
        PostResponse postResponse = postRepository.findPostResponseById(generatedPostId);

        return ApiResponse.<PostResponse>builder()
            .status(HttpStatus.CREATED.value())
            .message("Postingan berhasil dibuat")
            .data(postResponse)
            .build();
    }

    @Transactional
    public ApiResponse<PostResponse> editPost(Long postId, Long userId, PostRequest request) {
        log.info("Memproses edit postingan ID: {} untuk userId: {}", postId, userId);

        // Cari dulu postingan lama di database
        PostResponse existingPost;
        try {
            existingPost = postRepository.findPostResponseById(postId);
        } catch (Exception e) {
            log.error("Postingan dengan ID {} tidak ditemukan", postId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Postingan tidak ditemukan.");
        }

        // Validasi user yang mengedit adalah pemilik postingan tersebut
        if (!existingPost.getUser().getId().equals(userId)) {
            log.error("User {} mencoba mengedit postingan milik user {}", userId, existingPost.getUser().getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Anda tidak memiliki akses untuk mengedit postingan ini.");
        }

        String caption = request.getCaption();
        boolean hasCaption = caption != null && !caption.trim().isEmpty();
        boolean hasNewImage = request.getImage() != null && !request.getImage().isEmpty();

        if (!hasCaption && !hasNewImage && existingPost.getImage() == null) {
            log.error("Postingan tidak boleh kosong.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Postingan tidak boleh kosong. Harap isi teks atau unggah foto.");
        }

        // Ambil nilai lama sebagai default jika tidak ada perubahan gambar
        String imageUrl = existingPost.getImage();
        String imageId = existingPost.getImageId();

        // Logika penanganan gambar baru
        if (hasNewImage) {
            try {
                // Jika postingan lama punya gambar di Cloudinary, hapus dulu secara sinkron
                if (existingPost.getImageId() != null) {
                    log.info("Menghapus gambar lama di Cloudinary dengan Public ID: {}", existingPost.getImageId());

                    cloudinaryService.deletePostImage(existingPost.getImageId());
                }

                // Upload gambar baru
                Map uploadResult = cloudinaryService.uploadPostImage(request.getImage());
                imageUrl = (String) uploadResult.get("secure_url");
                imageId = (String) uploadResult.get("public_id");

                log.info("Sukses mengunggah gambar baru. URL: {}, Public ID: {}", imageUrl, imageId);

            } catch (Exception e) {
                log.error("Gagal memproses gambar saat edit postingan", e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"Other Error");
            }
        }

        // Update data ke database melalui repository
        boolean isUpdated = postRepository.update(
            postId,
            hasCaption ? caption.trim() : null,
            imageUrl,
            imageId);

        if (!isUpdated) {
            log.error("Gagal memperbarui data postingan di database.");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }

        // Ambil data yang sudah terupdate untuk dikembalikan ke client
        PostResponse updatedPostResponse = postRepository.findPostResponseById(postId);

        return ApiResponse.<PostResponse>builder()
            .status(HttpStatus.OK.value())
            .message("Postingan berhasil diperbarui")
            .data(updatedPostResponse)
            .build();
    }

    /**
     * Mengambil semua data postingan milik user tertentu dengan paginasi.
     */
    public ApiResponse<List<PostResponse>> getUserPosts(Long userId, int page, int size) {
        log.info("Mengambil postingan milik user ID: {} dengan page: {} dan size: {}", userId, page, size);

        // Hitung total data postingan milik user tersebut
        long totalElements = postRepository.countUserPosts(userId);

        // Hitung rumus offset database
        long offset = (long) page * size;

        // Tarik data dari repository
        List<PostResponse> posts = postRepository.findUserPosts(userId, size, offset);

        // Hitung total halaman
        int totalPages = (int) Math.ceil((double) totalElements / size);

        // Susun metadata paginasi ke dalam PagingResponse DTO
        PagingResponse paging = PagingResponse.builder()
            .currentPage(page)
            .size(size)
            .totalElements(totalElements)
            .totalPages(totalPages)
            .build();

        return ApiResponse.<List<PostResponse>>builder()
            .status(HttpStatus.OK.value())
            .message("Berhasil mengambil postingan user")
            .paging(paging)
            .data(posts)
            .build();
    }

    /**
     * Mengambil semua postingan dari seluruh user di aplikasi secara berurutan dan terpaginasi.
     */
    public ApiResponse<List<PostResponse>> getGlobalFeed(int page, int size) {
        log.info("Mengambil global feed. Page: {}, Size: {}", page, size);

        // Hitung total seluruh postingan yang ada di database aplikasi
        long totalElements = postRepository.countAllPostsGlobal();

        // Hitung rumus offset database
        long offset = (long) page * size;

        // Tarik potongan datanya dari repository
        List<PostResponse> posts = postRepository.findAllPostsGlobal(size, offset);

        // Hitung total halaman yang dihasilkan
        int totalPages = (int) Math.ceil((double) totalElements / size);

        // Bungkus metadata paginasi ke dalam PagingResponse DTO
        PagingResponse paging = PagingResponse.builder()
            .currentPage(page)
            .size(size)
            .totalElements(totalElements)
            .totalPages(totalPages)
            .build();

        return ApiResponse.<List<PostResponse>>builder()
            .status(HttpStatus.OK.value())
            .message("Berhasil mengambil global postingan")
            .paging(paging)
            .data(posts)
            .build();
    }

    /**
     * Mengambil detail satu postingan utuh sekaligus mengambil komentar di dalamnya terpaginasi.
     */
    public ApiResponse<PostDetailResponse> getPostDetail(Long postId, int commentPage, int commentSize) {

        log.info("Mengambil detail postingan ID: {} dengan paginasi komentar page: {}, size: {}", postId, commentPage, commentSize);

        // Ambil detail postingan utama.
        PostResponse post = postRepository.findPostResponseById(postId);

        // Hitung total elemen komentar khusus untuk post ID ini
        long totalComments = postRepository.countCommentsByPostId(postId);

        // Hitung rumus offset komentar
        long offset = (long) commentPage * commentSize;

        // Ambil list komentar dari repository
        List<PostCommentResponse> comments = postRepository.findCommentsByPostId(postId, commentSize, offset);

        // Hitung total halaman komentar
        int totalCommentPages = (int) Math.ceil((double) totalComments / commentSize);

        // Bungkus metadata paginasi komentar ke dalam PagingResponse
        PagingResponse paging = PagingResponse.builder()
            .currentPage(commentPage)
            .size(commentSize)
            .totalElements(totalComments)
            .totalPages(totalCommentPages)
            .build();

        // Satukan objek post, data komentar, dan paging ke dalam PostDetailResponse
        PostDetailResponse detailResponse = PostDetailResponse.builder()
            .post(post)
            .comments(comments)
            .paging(paging)
            .build();

        return ApiResponse.<PostDetailResponse>builder()
            .status(HttpStatus.OK.value())
            .message("Berhasil mengambil detail postingan")
            .data(detailResponse)
            .build();
    }

    /**
     * Menghapus postingan 
     */
    @Transactional
    public ApiResponse<String> deletePost(Long postId, Long userId) {
        log.info("Memproses penghapusan postingan ID: {} oleh userId: {}", postId, userId);

        // Cek keberadaan postingan
        PostResponse post;
        try {
            post = postRepository.findPostResponseById(postId);
        } catch (Exception e) {
            log.error("Postingan dengan ID {} tidak ditemukan untuk dihapus", postId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Postingan tidak ditemukan.");
        }

        // Validasi Kepemilikan
        if (!post.getUser().getId().equals(userId)) {
            log.error("User {} ilegal mencoba menghapus postingan milik user {}", userId, post.getUser().getId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Anda tidak memiliki akses untuk menghapus postingan ini.");
        }

        // Hapus gambar di Cloudinary 
        if (post.getImageId() != null && !post.getImageId().trim().isEmpty()) {
            try {
                log.info("Menghapus aset gambar di Cloudinary dengan ID: {}", post.getImageId());
                cloudinaryService.deletePostImage(post.getImageId());
            } catch (Exception e) {
                log.error("Gagal menghapus gambar di Cloudinary untuk post ID {}", postId, e);
            }
        }

        // Hapus post.
        boolean isDeleted = postRepository.deleteById(postId);
        if (!isDeleted) {
            log.error("Gagal menghapus baris data postingan di database.");
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }

        // Update data tabel user (Decrement post_count)
        log.debug("Menurunkan jumlah post_count untuk User ID {}", userId);
        boolean isDecrementSuccess = userRepository.decrementPostCount(userId);
        if (!isDecrementSuccess) {
            log.error("Gagal menurunkan post_count pada user ID {}", userId);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }

        return ApiResponse.<String>builder()
            .status(HttpStatus.OK.value())
            .message("Postingan berhasil dihapus")
            .data("Postingan berhasil dihapus")
            .build();
    }
    
}
