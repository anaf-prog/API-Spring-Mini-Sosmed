package com.sosmed.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import com.sosmed.dto.follow.FollowResponse;
import com.sosmed.dto.follow.UserFollowResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@RequiredArgsConstructor
@Slf4j
public class FollowRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    /**
     * RowMapper manual untuk memetakan ResultSet ke objek FollowResponse.
     */
    private final RowMapper<FollowResponse> followRowMapper = new RowMapper<FollowResponse>() {
        @Override
        public FollowResponse mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
            String userImageRaw = rs.getString("user_image");
            String usernameRaw = rs.getString("username");

            String userImageFinal = userImageRaw != null ? userImageRaw
                : "https://api.dicebear.com/7.x/initials/svg?seed=" + usernameRaw;

            UserFollowResponse user = UserFollowResponse.builder()
                .id(rs.getLong("user_id"))
                .fullname(rs.getString("fullname"))
                .username(usernameRaw)
                .image(userImageFinal)
                .build();

            FollowResponse follow = new FollowResponse();
            follow.setFollowingId(rs.getLong("following_id"));
            follow.setFollowerId(rs.getLong("follower_id"));

            if (rs.getTimestamp("created_at") != null) {
                follow.setCreatedAt(ZonedDateTime.ofInstant(rs.getTimestamp("created_at").toInstant(), ZoneId.systemDefault()));
            }
            if (rs.getTimestamp("updated_at") != null) {
                follow.setUpdatedAt(ZonedDateTime.ofInstant(rs.getTimestamp("updated_at").toInstant(), ZoneId.systemDefault()));
            }

            follow.setUser(user);
            return follow;
        }
    };

    public boolean isFollowing(Long followerId, Long followingId) {
        String sql = "SELECT COUNT(1) FROM follows WHERE follower_id = :followerId AND following_id = :followingId";
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("followerId", followerId)
            .addValue("followingId", followingId);

        Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    public void follow(Long followerId, Long followingId) {
        String sql = """
                        INSERT INTO follows (follower_id, following_id, created_at, updated_at)
                        VALUES (:followerId, :followingId, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("followerId", followerId)
            .addValue("followingId", followingId);

        try {
            namedParameterJdbcTemplate.update(sql, params);
            log.info("User ID {} berhasil follow User ID {}", followerId, followingId);
        } catch (Exception e) {
            log.error("Gagal melakukan insert follow: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }

    public void unfollow(Long followerId, Long followingId) {
        String sql = "DELETE FROM follows WHERE follower_id = :followerId AND following_id = :followingId";

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("followerId", followerId)
            .addValue("followingId", followingId);

        try {
            namedParameterJdbcTemplate.update(sql, params);
            log.info("User ID {} berhasil unfollow User ID {}", followerId, followingId);
        } catch (Exception e) {
            log.error("Gagal melakukan delete follow: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }

    /**
     * Menampilkan daftar FOLLOWER
     */
    public List<FollowResponse> findAllFollowers(Long userId, int page, int size) {
        String sql = """
                        SELECT
                            f.follower_id,
                            f.following_id,
                            f.created_at,
                            f.updated_at,

                            u.id AS user_id,
                            u.fullname,
                            u.username,
                            u.image AS user_image
                        FROM follows f
                        JOIN users u ON f.follower_id = u.id
                        WHERE f.following_id = :userId
                        ORDER BY f.created_at DESC
                        LIMIT :limit OFFSET :offset
                    """;

        int offset = page * size;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("limit", size)
            .addValue("offset", offset);

        try {
            return namedParameterJdbcTemplate.query(sql, params, followRowMapper);
        } catch (Exception e) {
            log.error("Gagal mengambil daftar Follower dari User ID {}: {}", userId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }

    /**
     * Menampilkan daftar FOLLOWING
     */
    public List<FollowResponse> findAllFollowing(Long userId, int page, int size) {
        String sql = """
                        SELECT
                            f.follower_id,
                            f.following_id,
                            f.created_at,
                            f.updated_at,

                            u.id AS user_id,
                            u.fullname,
                            u.username,
                            u.image AS user_image
                        FROM follows f
                        JOIN users u ON f.following_id = u.id
                        WHERE f.follower_id = :userId
                        ORDER BY f.created_at DESC
                        LIMIT :limit OFFSET :offset
                    """;

        int offset = page * size;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("userId", userId)
            .addValue("limit", size)
            .addValue("offset", offset);

        try {
            return namedParameterJdbcTemplate.query(sql, params, followRowMapper);
        } catch (Exception e) {
            log.error("Gagal mengambil daftar Following dari User ID {}: {}", userId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }

    /**
     * Fungsi count untuk kebutuhan totalElements & totalPages PagingResponse Followers
     */
    public long countFollowers(Long userId) {
        String sql = "SELECT COUNT(1) FROM follows WHERE following_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null ? count : 0L;
    }

    /**
     * Fungsi count untuk kebutuhan totalElements & totalPages PagingResponse Following
     */
    public long countFollowing(Long userId) {
        String sql = "SELECT COUNT(1) FROM follows WHERE follower_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
        Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null ? count : 0L;
    }
}
