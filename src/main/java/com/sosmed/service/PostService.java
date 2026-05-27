package com.sosmed.service;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sosmed.dto.ApiResponse;
import com.sosmed.dto.post.PostRequest;
import com.sosmed.dto.post.PostResponse;
import com.sosmed.repository.PostRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final PostRepository postRepository;
    private final CloudinaryService cloudinaryService;

    @Transactional
    public ApiResponse<PostResponse> createPost(Long userId, PostRequest request) {
        log.info("Memproses pembuatan postingan untuk userId: {}", userId);

        String caption = request.getCaption();
        boolean hasCaption = caption != null && !caption.trim().isEmpty();
        boolean hasImage = request.getImage() != null && !request.getImage().isEmpty();

        if (!hasCaption && !hasImage) {
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
                log.error("Proses pembuatan post terhenti karena gagal upload ke Cloudinary", e);
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Gagal membuat postingan.");
        }

        // Ambil data utuh hasil insert (termasuk join dengan data user) 
        PostResponse postResponse = postRepository.findPostResponseById(generatedPostId);

        return ApiResponse.<PostResponse>builder()
            .status(HttpStatus.CREATED.value())
            .message("Postingan berhasil dibuat")
            .data(postResponse)
            .build();
    }
    
}
