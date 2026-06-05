package com.sosmed.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import com.sosmed.dto.like.LikeInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@RequiredArgsConstructor
@Slf4j
public class LikeRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    /**
     * RowMapper manual untuk memetakan ResultSet ke objek LikeInfo.
     */
    private final RowMapper<LikeInfo> likeInfoRowMapper = new RowMapper<LikeInfo>() {
        @Override
        public LikeInfo mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
            Timestamp createdAtTimestamp = rs.getTimestamp("created_at");
            Timestamp updatedAtTimestamp = rs.getTimestamp("updated_at");

            ZonedDateTime createdAt = createdAtTimestamp != null
                ? ZonedDateTime.ofInstant(createdAtTimestamp.toInstant(), ZoneId.systemDefault())
                : null;
            ZonedDateTime updatedAt = updatedAtTimestamp != null
                ? ZonedDateTime.ofInstant(updatedAtTimestamp.toInstant(), ZoneId.systemDefault())
                : null;

            String userImageRaw = rs.getString("user_image");
            String usernameRaw = rs.getString("username");

            String userImageFinal = userImageRaw != null ? userImageRaw
                : "https://api.dicebear.com/7.x/initials/svg?seed=" + usernameRaw;

            return LikeInfo.builder()
                .postId(rs.getLong("post_id"))
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .userId(rs.getLong("user_id"))
                .fullname(rs.getString("fullname"))
                .username(usernameRaw)
                .userImage(userImageFinal)
                .build();
        }
    };

    /**
     * Memeriksa apakah user sudah memberikan like pada postingan tertentu.
     */
    public boolean existsByUserIdAndPostId(Long userId, Long postId) {
        String sql = "SELECT COUNT(1) FROM likes WHERE user_id = :userId AND post_id = :postId";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("postId", postId);

        try {
            Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("Gagal memeriksa eksistensi like untuk userId {} dan postId {}: {}", userId, postId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }

    /**
     * Menyimpan data like baru ke tabel likes.
     */
    public void save(Long userId, Long postId) {
        String sql = """
                        INSERT INTO likes (user_id, post_id, created_at, updated_at)
                        VALUES (:userId, :postId, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("postId", postId);

        try {
            namedParameterJdbcTemplate.update(sql, params);
        } catch (Exception e) {
            log.error("Gagal menyimpan data like untuk userId {} dan postId {}: {}", userId, postId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }

    /**
     * Menghapus data like dari tabel likes (Unlike).
     */
    public void delete(Long userId, Long postId) {
        String sql = "DELETE FROM likes WHERE user_id = :userId AND post_id = :postId";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("postId", postId);

        try {
            namedParameterJdbcTemplate.update(sql, params);
        } catch (Exception e) {
            log.error("Gagal menghapus data like untuk userId {} dan postId {}: {}", userId, postId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }

    /**
     * Mengambil detail relasi like setelah berhasil operasi untuk membangun response.
     */
    public Optional<LikeInfo> findLikeInfo(Long userId, Long postId) {
        String sql = """
                        SELECT 
                            l.post_id,
                            l.created_at,
                            l.updated_at,

                            u.id AS user_id,
                            u.fullname,
                            u.username,
                            u.image AS user_image
                        FROM likes l
                        INNER JOIN users u ON l.user_id = u.id
                        WHERE l.user_id = :userId AND l.post_id = :postId
                    """;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("postId", postId);

        try {
            List<LikeInfo> results = namedParameterJdbcTemplate.query(sql, params, likeInfoRowMapper);
            return results.stream().findFirst();
        } catch (Exception e) {
            log.error("Gagal mengambil info like untuk userId {} dan postId {}: {}", userId, postId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }

    /**
     * Menghitung total user yang menyukai satu postingan tertentu Untuk Paginasi.
     */
    public long countLikesByPostId(Long postId) {
        String sql = "SELECT COUNT(1) FROM likes WHERE post_id = :postId";
        MapSqlParameterSource params = new MapSqlParameterSource("postId", postId);

        try {
            Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
            return count != null ? count : 0L;
        } catch (Exception e) {
            log.error("Gagal menghitung total likes untuk post ID {}: {}", postId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }

    /**
     * Mengambil daftar informasi user yang menyukai postingan dengan Paginasi.
     */
    public List<LikeInfo> findLikesByPostIdWithPaging(Long postId, int limit, long offset) {
        String sql = """
                        SELECT 
                            l.post_id,
                            l.created_at,
                            l.updated_at,
                            
                            u.id AS user_id,
                            u.fullname,
                            u.username,
                            u.image AS user_image
                        FROM likes l
                        INNER JOIN users u ON l.user_id = u.id
                        WHERE l.post_id = :postId
                        ORDER BY l.created_at DESC
                        LIMIT :limit OFFSET :offset
                    """;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("postId", postId)
            .addValue("limit", limit)
            .addValue("offset", offset);

        try {
            return namedParameterJdbcTemplate.query(sql, params, likeInfoRowMapper);
        } catch (Exception e) {
            log.error("Gagal mengambil daftar user likes untuk post ID {} dengan paginasi: {}", postId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }
    
}
