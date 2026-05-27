package com.sosmed.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.sosmed.dto.post.PostResponse;
import com.sosmed.dto.post.UserPostResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@RequiredArgsConstructor
@Slf4j
public class PostRepository { 

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    /**
     * Menyimpan postingan baru ke dalam database dan mengembalikan ID yang digenerate.
     */
    public Long save(Long userId, String caption, String image, String imageId) {
        String sql = """
                        INSERT INTO posts (user_id, caption, image, image_id, comment_count, like_count, created_at, updated_at)
                        VALUES (:userId, :caption, :image, :imageId, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("caption", caption)
            .addValue("image", image)
            .addValue("imageId", imageId);

        KeyHolder keyHolder = new GeneratedKeyHolder();

        try {
            namedParameterJdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});
            Number key = keyHolder.getKey();
            return key != null ? key.longValue() : null;

        } catch (Exception e) {
            log.error("Gagal menyimpan postingan baru: {}", e.getMessage(), e);
            throw new RuntimeException("Other Error", e);
        }
    }

    /**
     * Mengambil detail data Post beserta informasi User berdasarkan Post ID.
     */
    public PostResponse findPostResponseById(Long postId) {
        String sql = """
                        SELECT
                            p.id AS post_id,
                            p.caption,
                            p.image AS post_image,
                            p.image_id AS post_image_id,
                            p.comment_count,
                            p.like_count,
                            p.created_at AS post_created_at,
                            p.updated_at AS post_updated_at,
                            
                            u.id AS user_id,
                            u.fullname,
                            u.username,
                            u.image AS user_image
                        FROM posts p
                        JOIN users u ON p.user_id = u.id
                        WHERE p.id = :postId
                    """;

        MapSqlParameterSource params = new MapSqlParameterSource("postId", postId);

        try {
            return namedParameterJdbcTemplate.queryForObject(sql, params, (rs, rowNum) -> mapRowToPostResponse(rs));
        } catch (Exception e) {
            log.error("Gagal mengambil detail postingan untuk ID {}: {}", postId, e.getMessage(), e);
            throw new RuntimeException("Postingan tidak ditemukan", e);
        }
    }

    /**
     * Query : Menghitung total seluruh postingan berdasarkan daftar ID user yang diperbolehkan (diikuti).
     * Ini digunakan untuk mengisi informasi totalElements dan totalPages pada paginasi hybrid.
     */
    public long countTimelinePosts(List<Long> allowedUserIds) {
        // Antisipasi jika daftar ID kosong, langsung kembalikan 0 tanpa menembak database
        if (allowedUserIds == null || allowedUserIds.isEmpty()) {
            return 0L;
        }

        String sql = "SELECT COUNT(1) FROM posts WHERE user_id IN (:allowedUserIds)";

        MapSqlParameterSource params = new MapSqlParameterSource("allowedUserIds", allowedUserIds);

        try {
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.error("Gagal melakukan query countTimelinePosts: {}", e.getMessage(), e);
            throw new RuntimeException("Other Error", e);
        }
    }

    /**
     * Query : Mengambil potongan data postingan beserta data user (JOIN) menggunakan limit dan offset.
     */
    public List<PostResponse> findTimelinePosts(List<Long> allowedUserIds, int limit, long offset) {
        // Antisipasi jika daftar ID kosong, langsung kembalikan list kosong
        if (allowedUserIds == null || allowedUserIds.isEmpty()) {
            return List.of();
        }

        String sql = """
                        SELECT 
                            posts.id AS post_id,
                            posts.caption,
                            posts.image AS post_image,
                            posts.image_id AS post_image_id,
                            posts.comment_count,
                            posts.like_count,
                            posts.created_at AS post_created_at,
                            posts.updated_at AS post_updated_at,
                            
                            users.id AS user_id,
                            users.fullname,
                            users.username,
                            users.image AS user_image
                        FROM posts
                        INNER JOIN users ON posts.user_id = users.id
                        WHERE posts.user_id IN (:allowedUserIds)
                        ORDER BY posts.created_at DESC
                        LIMIT :limit OFFSET :offset
                    """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("allowedUserIds", allowedUserIds)
                .addValue("limit", limit)
                .addValue("offset", offset);

        try {
            return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> mapRowToPostResponse(rs));
        } catch (Exception e) {
            log.error("Gagal melakukan query findTimelinePosts: {}", e.getMessage(), e);
            throw new RuntimeException("Other Error", e);
        }
    }

    /**
     * Helper method privat untuk memetakan baris ResultSet database ke Objek DTO Java
     */
    private PostResponse mapRowToPostResponse(ResultSet rs) throws SQLException {

        String userImageRaw = rs.getString("user_image");
        String usernameRaw = rs.getString("username");

        // Logika Fallback Dicebear disatukan di sini agar semua endpoint mendapatkan
        // penanganan yang sama
        String userImageFinal = userImageRaw != null ? userImageRaw
                : "https://api.dicebear.com/7.x/initials/svg?seed=" + usernameRaw;
        
        // 1. Ekstrak data user dan masukkan ke UserPostResponse DTO
        UserPostResponse user = UserPostResponse.builder()
            .id(rs.getLong("user_id"))
            .fullname(rs.getString("fullname"))
            .username(rs.getString("username"))
            .image(userImageFinal)
            .build();

        // 2. Ekstrak data post dan pasang objek user di dalamnya
        return PostResponse.builder()
            .id(rs.getLong("post_id"))
            .caption(rs.getString("caption"))
            .image(rs.getString("post_image"))
            .imageId(rs.getString("post_image_id"))
            .commentCount(rs.getLong("comment_count"))
            .likeCount(rs.getLong("like_count"))
            .createdAt(rs.getTimestamp("post_created_at") != null ? 
                    ZonedDateTime.ofInstant(rs.getTimestamp("post_created_at").toInstant(), ZoneId.systemDefault()) : null)
            .updatedAt(rs.getTimestamp("post_updated_at") != null ? 
                    ZonedDateTime.ofInstant(rs.getTimestamp("post_updated_at").toInstant(), ZoneId.systemDefault()) : null)
            .user(user) // Menggabungkan relasi JOIN
            .build();
    }
    
}
