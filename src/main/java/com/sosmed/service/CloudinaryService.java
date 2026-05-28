package com.sosmed.service;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.sosmed.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private final UserRepository userRepository;

    /**
     * Mengunggah foto profil baru, menghapus foto lama di cloud,
     * lalu memperbarui database secara asinkron lewat Virtual Thread.
     */
    @Async("cloudinaryVirtualThreadExecutor")
    public void uploadAndHandleOldImageAsync(Long userId, byte[] fileBytes, String oldImageId) {
        try {
            log.info("Memulai pemrosesan media profil untuk user id: {}", userId);

            // 1. Upload gambar baru ke Cloudinary 
            Map uploadResult = cloudinary.uploader().upload(fileBytes, ObjectUtils.emptyMap());
            String newUrl = (String) uploadResult.get("secure_url");
            String newImageId = (String) uploadResult.get("public_id");

            log.info("Sukses upload file baru ke Cloudinary. Public ID Baru: {}", newImageId);

            // 2. Update informasi URL dan Image ID baru tersebut ke database Postgres
            userRepository.updateUserImage(userId, newUrl, newImageId);

            // 3. Jika ada image ID lama di database, hapus berkas lamanya dari Cloudinary
            if (oldImageId != null && !oldImageId.trim().isEmpty()) {
                log.info("Menghapus foto profil lama dari Cloudinary dengan Public ID: {}", oldImageId);

                Map deleteResult = cloudinary.uploader().destroy(oldImageId, ObjectUtils.emptyMap());

                log.info("Hasil penghapusan foto lama dari Cloudinary: {}", deleteResult.get("result"));
            }

        } catch (Exception e) {
            log.error("Terjadi kegagalan pada pemrosesan Cloudinary asinkron untuk user ID {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Mengunggah gambar postingan secara SINKRON.
     * Digunakan karena data URL dan Public ID diperlukan langsung sebelum data post disimpan ke database.
     */
    public Map uploadPostImage(MultipartFile file) {
        try {
            log.info("Mengunggah gambar postingan ke Cloudinary");
            return cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());

        } catch (Exception e) {
            log.error("Gagal mengunggah gambar postingan ke Cloudinary: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error.");
        }
    }

    /**
     * Menghapus gambar postingan secara SINKRON dari Cloudinary berdasarkan Public
     * ID.
     * Digunakan saat edit/update postingan atau hapus postingan.
     */
    public Map deletePostImage(String oldImageId) {
        try {
            log.info("Menghapus gambar postingan dari Cloudinary untuk Public ID: {}", oldImageId);
            return cloudinary.uploader().destroy(oldImageId, ObjectUtils.emptyMap());

        } catch (Exception e) {
            log.error("Gagal menghapus gambar postingan dari Cloudinary untuk Public ID {}: {}", oldImageId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error.");
        }
    }
    
}
