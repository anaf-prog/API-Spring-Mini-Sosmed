package com.sosmed.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import com.sosmed.dto.comment.CommentResponse;
import com.sosmed.dto.comment.UserCommentResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CommentRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    /**
     * Menyimpan komentar baru dan mengembalikan ID yang digenerate.
     */
    public Long save(Long userId, Long postId, String content, String image, String imageId) {
        String sql = """
                        INSERT INTO comments (user_id, post_id, content, image, image_id, created_at, updated_at)
                        VALUES (:userId, :postId, :content, :image, :imageId, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("postId", postId)
            .addValue("content", content)
            .addValue("image", image)
            .addValue("imageId", imageId);

        KeyHolder keyHolder = new GeneratedKeyHolder();

        try {
            namedParameterJdbcTemplate.update(sql, params, keyHolder, new String[]{"id"});
            Number key = keyHolder.getKey();
            return key != null ? key.longValue() : null;
        } catch (Exception e) {
            log.error("Gagal menyimpan komentar baru ke database: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }

    /**
     * Mengambil data komentar yang baru saja disimpan beserta info usernya untuk response.
     */
    public CommentResponse findCommentResponseById(Long commentId) {
        String sql = """
                        SELECT 
                            c.id AS comment_id,
                            c.post_id,
                            c.content,
                            c.image AS comment_image,
                            c.image_id AS comment_image_id,
                            c.created_at AS comment_created_at,
                            c.updated_at AS comment_updated_at,

                            u.id AS user_id,
                            u.fullname,
                            u.username,
                            u.image AS user_image
                        FROM comments c
                        JOIN users u ON c.user_id = u.id
                        WHERE c.id = :commentId
                    """;

        MapSqlParameterSource params = new MapSqlParameterSource("commentId", commentId);

        try {
            return namedParameterJdbcTemplate.queryForObject(sql, params, (rs, rowNum) -> mapRowToCommentResponse(rs));
        } catch (Exception e) {
            log.error("Gagal mengambil data response komentar ID {}: {}", commentId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }

    /**
     * Memperbarui data postingan yang sudah ada berdasarkan commentId.
     */
    public boolean update(Long commentId, String content, String image, String imageId) {
        String sql = """
                        UPDATE comments
                        SET content = :content,
                            image = :image,
                            image_id = :imageId,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = :commentId
                    """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("commentId", commentId)
            .addValue("content", content)
            .addValue("image", image)
            .addValue("imageId", imageId);

        try {
            int rowsAffected = namedParameterJdbcTemplate.update(sql, params);
            return rowsAffected > 0;
        } catch (Exception e) {
            log.error("Gagal memperbarui data comment ID {}: {}", commentId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }

    /**
     * Menghapus data komentar berdasarkan ID.
     */
    public boolean deleteById(Long commentId) {
        String sql = "DELETE FROM comments WHERE id = :commentId";
        MapSqlParameterSource params = new MapSqlParameterSource("commentId", commentId);
        try {
            int rowsAffected = namedParameterJdbcTemplate.update(sql, params);
            return rowsAffected > 0;
        } catch (Exception e) {
            log.error("Gagal menghapus komentar ID {}: {}", commentId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }

    private CommentResponse mapRowToCommentResponse(ResultSet rs) throws SQLException {
        String userImageRaw = rs.getString("user_image");
        String usernameRaw = rs.getString("username");

        String userImageFinal = userImageRaw != null ? userImageRaw
            : "https://api.dicebear.com/7.x/initials/svg?seed=" + usernameRaw;

        UserCommentResponse user = UserCommentResponse.builder()
            .id(rs.getLong("user_id"))
            .fullname(rs.getString("fullname"))
            .username(rs.getString("username"))
            .image(userImageFinal)
            .build();

        return CommentResponse.builder()
            .id(rs.getLong("comment_id"))
            .postId(rs.getLong("post_id"))
            .content(rs.getString("content"))
            .image(rs.getString("comment_image"))
            .imageId(rs.getString("comment_image_id"))
            .createdAt(rs.getTimestamp("comment_created_at") != null ?
                ZonedDateTime.ofInstant(rs.getTimestamp("comment_created_at").toInstant(), ZoneId.systemDefault()) : null)
            .updatedAt(rs.getTimestamp("comment_updated_at") != null ?
                ZonedDateTime.ofInstant(rs.getTimestamp("comment_updated_at").toInstant(), ZoneId.systemDefault()) : null)
            .user(user)
            .build();
    }
    
}
