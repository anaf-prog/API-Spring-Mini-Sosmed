package com.sosmed.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import com.sosmed.dto.bookmark.BookmarkInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@RequiredArgsConstructor
@Slf4j
public class BookmarkRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    /**
     * Memeriksa apakah user sudah memberikan like pada postingan tertentu.
     */
    public boolean existByUserIdAndPostId(Long userId, Long postId) {
        String sql = "SELECT COUNT(1) FROM bookmarks WHERE user_id = :userId AND post_id = :postId ";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("postId", postId);

        try {
            Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("Gagal memeriksa bookmark untuk userId {} dan postId {}: {}", userId, postId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }    
    }

    /**
     * Menyimpan data bookmark baru ke tabel bookmarks.
     */
    public void save(Long userId, Long postId) {
        String sql = """
                        INSERT INTO bookmarks (user_id, post_id, created_at, updated_at)
                        VALUES (:userId, :postId, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("postId", postId);

        try {
            namedParameterJdbcTemplate.update(sql, params);
        } catch (Exception e) {
            log.error("Gagal menyimpan data bookmark untuk userId {} dan postId {}: {}", userId, postId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }

    /**
     * Hapus data bookmark.
     */
    public void delete(Long userId, Long postId) {
        String sql = "DELETE FROM bookmarks WHERE user_id = :userId AND post_id = :postId";

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("postId", postId);

        try {
            namedParameterJdbcTemplate.update(sql, params);
        } catch (Exception e) {
            log.error("Gagal hapus data bookmark untuk userId {} dan postId {}: {}", userId, postId, e.getMessage(),e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }    
    }

    /**
     * Mengambil detail relasi bookmark setelah berhasil operasi untuk membangun response.
     */
    public Optional<BookmarkInfo> findBookmarkInfo(Long userId, Long postId) {
        String sql = """
                        SELECT 
                            b.post_id,
                            b.created_at,
                            b.updated_at,

                            u.id AS user_id,
                            u.fullname,
                            u.username,
                            u.image AS user_image
                        FROM bookmarks b
                        INNER JOIN users u ON b.user_id = u.id
                        WHERE b.user_id = :userId AND b.post_id = :postId
                    """;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("postId", postId);

        try {
            BookmarkInfo info = namedParameterJdbcTemplate.query(sql, params, rs -> {
                if (rs.next()) {
                    return mapRowToBookmarkInfo(rs);
                }
                return null;
            });
            return Optional.ofNullable(info);
        } catch (Exception e) {
            log.error("Gagal mengambil info bookmark untuk userId {} dan postId {}: {}", userId, postId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }

    /**
     * Menghitung total user yang membookmark satu postingan tertentu Untuk Paginasi.
     */
    public long countBookmarksByPostId(Long postId) {
        String sql = "SELECT COUNT(1) FROM bookmarks WHERE post_id = :postId";
        MapSqlParameterSource params = new MapSqlParameterSource("postId", postId);

        try {
           Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
           return count != null ? count : 0L; 
        } catch (Exception e) {
            log.error("Gagal menghitung total bookmark untuk post ID {}: {}", postId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }

    }

    /**
     * Mengambil daftar informasi user yang membookmark postingan dengan Paginasi.
     */
    public List<BookmarkInfo> findBookmarkByPostIdWithPaging(Long postId, int limit, long offset) {
        String sql = """
                        SELECT
                            b.post_id,
                            b.created_at,
                            b.updated_at,

                            u.id AS user_id,
                            u.fullname,
                            u.username,
                            u.image AS user_image
                        FROM bookmarks b
                        INNER JOIN users u ON b.user_id = u.id
                        WHERE b.post_id = :postId
                        ORDER BY b.created_at DESC
                        LIMIT :limit OFFSET :offset    
                    """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("postId", postId)
            .addValue("limit", limit)
            .addValue("offset", offset);
            
        try {
           return namedParameterJdbcTemplate.query(sql, params, (rs, rowNum) -> mapRowToBookmarkInfo(rs)); 
        } catch (Exception e) {
            log.error("Gagal mengambil daftar user bookmark untuk post ID {} dengan paginasi: {}", postId, e.getMessage(),e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }    
    }

    private BookmarkInfo mapRowToBookmarkInfo(ResultSet rs) throws SQLException {
        Timestamp createdAtTimestamp = rs.getTimestamp("created_at");
        Timestamp updatedAtTimestamp = rs.getTimestamp("updated_at");

        ZonedDateTime createdAt = createdAtTimestamp != null 
            ? ZonedDateTime.ofInstant(createdAtTimestamp.toInstant(), ZoneId.systemDefault()) 
            : null;
        ZonedDateTime updatedAt = updatedAtTimestamp != null 
            ? ZonedDateTime.ofInstant(updatedAtTimestamp.toInstant(), ZoneId.systemDefault()) 
            : null;

        return BookmarkInfo.builder()
            .postId(rs.getLong("post_id"))
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .userId(rs.getLong("user_id"))
            .fullname(rs.getString("fullname"))
            .username(rs.getString("username"))
            .userImage(rs.getString("user_image"))
            .build();
    }
    
}
