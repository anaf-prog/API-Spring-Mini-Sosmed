package com.sosmed.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import com.sosmed.dto.post.PostCommentResponse;
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }

    /**
     * Memperbarui data postingan yang sudah ada berdasarkan postId.
     */
    public boolean update(Long postId, String caption, String image, String imageId) {
        String sql = """
                        UPDATE posts
                        SET caption = :caption,
                            image = :image,
                            image_id = :imageId,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = :postId
                    """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("postId", postId)
            .addValue("caption", caption)
            .addValue("image", image)
            .addValue("imageId", imageId);

        try {
            int rowsAffected = namedParameterJdbcTemplate.update(sql, params);
            return rowsAffected > 0;
        } catch (Exception e) {
            log.error("Gagal memperbarui data postingan ID {}: {}", postId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }

    /**
     * Query Menghitung total postingan khusus milik satu spesifik user.
     */
    public long countUserPosts(Long userId) {
        String sql = "SELECT COUNT(1) FROM posts WHERE user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);

        try {
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.error("Gagal menghitung postingan milik user ID {}: {}", userId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }

    /**
     * Query Mengambil list postingan milik spesifik user dengan Paginasi (LIMIT & OFFSET).
     */
    public List<PostResponse> findUserPosts(Long userId, int limit, long offset) {
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
                        WHERE posts.user_id = :userId
                        ORDER BY posts.created_at DESC
                        LIMIT :limit OFFSET :offset
                    """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("limit", limit)
            .addValue("offset", offset);

        try {
            return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> mapRowToPostResponse(rs));
        } catch (Exception e) {
            log.error("Gagal mengambil list postingan milik user ID {}: {}", userId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }

    /**
     * Menghitung total semua postingan yang ada di database aplikasi tanpa filter apa pun.
     */
    public long countAllPostsGlobal() {
        String sql = "SELECT COUNT(1) FROM posts";
        try {
            Long count = namedParameterJdbcTemplate.queryForObject(sql, new MapSqlParameterSource(), Long.class);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.error("Gagal menghitung total semua postingan global: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }

    /**
     * Mengambil data postingan milik semua orang + diri sendiri yang di-JOIN dengan data user,
     * lalu diurutkan dari yang paling baru menggunakan LIMIT dan OFFSET.
     */
    public List<PostResponse> findAllPostsGlobal(int limit, long offset) {
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
                        ORDER BY posts.created_at DESC
                        LIMIT :limit OFFSET :offset
                    """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("limit", limit)
            .addValue("offset", offset);

        try {
            return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> mapRowToPostResponse(rs));
        } catch (Exception e) {
            log.error("Gagal mengambil data postingan global feed: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }

    /**
     * Menghitung total komentar yang ada di dalam satu postingan tertentu.
     */
    public long countCommentsByPostId(Long postId) {
        String sql = "SELECT COUNT(1) FROM comments WHERE post_id = :postId";
        MapSqlParameterSource params = new MapSqlParameterSource("postId", postId);
        try {
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.error("Gagal menghitung total komentar post ID {}: {}", postId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }

    /**
     * Mengambil potongan list komentar yang di-JOIN dengan user komentator
     */
    public List<PostCommentResponse> findCommentsByPostId(Long postId, int limit, long offset) {
        String sql = """
                        SELECT
                            c.id AS comment_id,
                            c.content AS comment_content,
                            c.created_at AS comment_created_at,
                            c.updated_at AS comment_updated_at,

                            u.id AS user_id,
                            u.fullname,
                            u.username,
                            u.image AS user_image
                        FROM comments c
                        INNER JOIN users u ON c.user_id = u.id
                        WHERE c.post_id = :postId
                        ORDER BY c.created_at DESC
                        LIMIT :limit OFFSET :offset
                    """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("postId", postId)
            .addValue("limit", limit)
            .addValue("offset", offset);

        try {
            return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> PostCommentResponse.builder()
                .id(rs.getLong("comment_id"))
                .content(rs.getString("comment_content"))
                .createdAt(rs.getTimestamp("comment_created_at") != null
                    ? ZonedDateTime.ofInstant(rs.getTimestamp("comment_created_at").toInstant(), ZoneId.systemDefault())
                    : null)
                .updatedAt(rs.getTimestamp("comment_updated_at") != null
                    ? ZonedDateTime.ofInstant(rs.getTimestamp("comment_updated_at").toInstant(), ZoneId.systemDefault())
                    : null)
                .user(UserPostResponse.builder()
                    .id(rs.getLong("user_id"))
                    .fullname(rs.getString("fullname"))
                    .username(rs.getString("username"))
                    .image(rs.getString("user_image") != null ? rs.getString("user_image")
                        : "https://api.dicebear.com/7.x/initials/svg?seed=" + rs.getString("username"))
                    .build())
                .build());
        } catch (Exception e) {
            log.error("Gagal mengambil komentar untuk post ID {}: {}", postId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }

    /**
     * Menghapus baris data postingan utama berdasarkan ID.
     */
    public boolean deleteById(Long postId) {
        String sql = "DELETE FROM posts WHERE id = :postId";
        MapSqlParameterSource params = new MapSqlParameterSource("postId", postId);
        try {
            int rowsAffected = namedParameterJdbcTemplate.update(sql, params);
            return rowsAffected > 0;
        } catch (Exception e) {
            log.error("Gagal menghapus baris postingan ID {}: {}", postId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }

    /**
     * Helper method privat untuk memetakan baris ResultSet database ke Objek DTO Java
     */
    private PostResponse mapRowToPostResponse(ResultSet rs) throws SQLException {

        String userImageRaw = rs.getString("user_image");
        String usernameRaw = rs.getString("username");

        // Logika Fallback Dicebear 
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
