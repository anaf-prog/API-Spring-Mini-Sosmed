package com.sosmed.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import com.sosmed.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    /**
     * RowMapper manual untuk memetakan ResultSet database ke Objek User secara eksplisit.
     */
    private final RowMapper<User> userRowMapper = new RowMapper<User>() {
        @Override
        public User mapRow(@NonNull ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setId(rs.getLong("id"));
            user.setFullname(rs.getString("fullname"));
            user.setUsername(rs.getString("username"));
            user.setEmail(rs.getString("email"));
            user.setPassword(rs.getString("password"));
            user.setBio(rs.getString("bio"));
            user.setImage(rs.getString("image"));
            user.setImageId(rs.getString("image_id"));
            user.setFollowingCount(rs.getLong("following_count"));
            user.setFollowerCount(rs.getLong("follower_count"));
            user.setPostCount(rs.getLong("post_count"));
            user.setBookmarkCount(rs.getLong("bookmark_count"));

            if (rs.getTimestamp("created_at") != null) {
                user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            }
            if (rs.getTimestamp("updated_at") != null) {
                user.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            }
            
            return user;
        }
    };

    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(1) FROM users WHERE email = :email";

        // Bungkus parameter ke dalam MapSqlParameterSource
        MapSqlParameterSource params = new MapSqlParameterSource("email", email);

        Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(1) FROM users WHERE username = :username";

        MapSqlParameterSource params = new MapSqlParameterSource("username", username);

        Integer count = namedParameterJdbcTemplate.queryForObject(sql, params, Integer.class);
        return count != null && count > 0;
    }

    public User creatUser(User user) {
        String sql = """
                        INSERT INTO users (fullname, username, email, password, bio, image, image_id)
                        VALUES (:fullname, :username, :email, :password, :bio, :image, :imageId)
                        RETURNING id, fullname, username, email, password, bio, image, image_id, following_count, follower_count, post_count, bookmark_count, created_at, updated_at
                    """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("fullname", user.getFullname())
            .addValue("username", user.getUsername())
            .addValue("email", user.getEmail())
            .addValue("password", user.getPassword())
            .addValue("bio", user.getBio())
            .addValue("image", user.getImage())
            .addValue("imageId", user.getImageId());

        try {
            return namedParameterJdbcTemplate.queryForObject(
                sql,
                params,
                userRowMapper);

        } catch (Exception e) {
            log.error("Gagal melakukan insert user: {}", e.getMessage());
            throw new RuntimeException("Other Error", e);
        }
    }

    public Optional<User> findByUsernameOrEmail(String usernameOrEmail) {
        String sql = "SELECT * FROM users WHERE username = :usernameOrEmail OR email = :usernameOrEmail";

        MapSqlParameterSource params = new MapSqlParameterSource("usernameOrEmail", usernameOrEmail);

        try {

            User user = namedParameterJdbcTemplate.queryForObject(
                sql,
                params,
                userRowMapper);
            return Optional.ofNullable(user);

        } catch (Exception e) {
            log.error("Gagal melakukan query findByUsernameOrEmail : {}", e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = :id";

        MapSqlParameterSource params = new MapSqlParameterSource("id", id);

        try {

            return namedParameterJdbcTemplate.query(sql, params, userRowMapper)
                .stream()
                .findFirst();

        } catch (Exception e) {
            log.error("Gagal melakukan query findById : {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Mengupdate data user secara dinamis (Hanya kolom yang tidak null yang diupdate).
     */
    public void updateUser(Long userId, User user) {

        StringBuilder sql = new StringBuilder("UPDATE users SET ");
        MapSqlParameterSource params = new MapSqlParameterSource();
        List<String> columns = new ArrayList<>();

        if (user.getFullname() != null && !user.getFullname().trim().isEmpty()) {
            columns.add("fullname = :fullname");
            params.addValue("fullname", user.getFullname());
        }
 
        if (user.getUsername() != null && !user.getUsername().trim().isEmpty()) {
            columns.add("username = :username");
            params.addValue("username", user.getUsername());
        }

        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            columns.add("email = :email");
            params.addValue("email", user.getEmail());
        }

        if (user.getPassword() != null && !user.getPassword().trim().isEmpty()) {
            columns.add("password = :password");
            params.addValue("password", user.getPassword());
        }

        if (user.getBio() != null && !user.getBio().trim().isEmpty()) {
            columns.add("bio = :bio");
            params.addValue("bio", user.getBio());
        }

        if (columns.isEmpty()) {
            return;
        }

        columns.add("updated_at = CURRENT_TIMESTAMP");
        sql.append(String.join(", ", columns));
        sql.append(" WHERE id = :id");
        params.addValue("id", userId);

        try {
            namedParameterJdbcTemplate.update(sql.toString(), params);
            log.info("Berhasil melakukan update data untuk user ID: {}", userId);

        } catch (Exception e) {
            log.error("Gagal melakukan update data user ID {}: {}", userId, e.getMessage());
            throw new RuntimeException("Other Error", e);
        }

    }

    /**
     * Memperbarui informasi gambar user di database secara asinkron menggunakan Virtual Thread.
     */
    @Async("cloudinaryVirtualThreadExecutor")
    public void updateUserImage(Long userId, String imageUrl, String imageId) {
        String sql = "UPDATE users SET image = :image, image_id = :imageId, updated_at = CURRENT_TIMESTAMP WHERE id = :id";
        
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("image", imageUrl)
            .addValue("imageId", imageId)
            .addValue("id", userId);

        try {
            namedParameterJdbcTemplate.update(sql.toString(), params);
            log.info("Berhasil update gambar untuk user ID: {}", userId);

        } catch (Exception e) {
            log.error("Gagal memperbarui info gambar di database untuk user ID {}: {}", userId, e.getMessage());
        }
    }

    // ================== Paginasi data user ====================

    public long countAllUsers() {

        String sql = "SELECT COUNT(1) FROM users";

        // Karena tidak ada parameter dinamis di query ini, kirim EmptyMap/MapSqlParameterSource kosong
        MapSqlParameterSource params = new MapSqlParameterSource();

        Long count = namedParameterJdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null ? count : 0L;
    }

    public List<User> findAllWithPagination(int limit, int offset) {
        
        String sql = "SELECT * FROM users ORDER BY created_at DESC LIMIT :limit OFFSET :offset";

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("limit", limit)
            .addValue("offset", offset);

        return namedParameterJdbcTemplate.query(sql, params, userRowMapper);
    }

    /**
     * Menaikkan jumlah post_count
     */
    public boolean incrementPostCount(Long userId) {
        String sql = """
                        UPDATE users
                        SET post_count = post_count + 1,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = :userId
                    """;

        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);

        try {
            int rowsAffected = namedParameterJdbcTemplate.update(sql, params);
            return rowsAffected > 0;
        } catch (Exception e) {
            log.error("Gagal increment post_count untuk User ID {}: {}", userId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }

    /**
     * Menurunkan jumlah post_count
     * Dipastikan nilainya tidak akan minus di bawah 0 menggunakan fungsi GREATEST.
     */
    public boolean decrementPostCount(Long userId) {
        String sql = """
                        UPDATE users
                        SET post_count = GREATEST(post_count - 1, 0),
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = :userId
                    """;

        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);

        try {
            int rowsAffected = namedParameterJdbcTemplate.update(sql, params);
            return rowsAffected > 0;
        } catch (Exception e) {
            log.error("Gagal decrement post_count untuk User ID {}: {}", userId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }

    /**
     * Menaikkan jumlah following_count
     */
    public boolean incrementFollowingCount(Long userId) {
        String sql = """
                        UPDATE users
                        SET following_count = following_count + 1,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = :userId
                    """;

        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);

        try {
            int rowsAffected = namedParameterJdbcTemplate.update(sql, params);
            return rowsAffected > 0;
        } catch (Exception e) {
            log.error("Gagal increment following_count untuk User ID {}: {}", userId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }

    /**
     * Menurunkan jumlah Following_count
     */
    public boolean decrementFollowingCount(Long userId) {
        String sql = """
                        UPDATE users
                        SET following_count = GREATEST(following_count - 1, 0),
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = :userId
                    """;

        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);

        try {
            int rowsAffected = namedParameterJdbcTemplate.update(sql, params);
            return rowsAffected > 0;
        } catch (Exception e) {
            log.error("Gagal decrement following_count untuk User ID {}: {}", userId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }

    /**
     * Menaikkan jumlah follower_count
     */
    public boolean incrementFollowerCount(Long userId) {
        String sql = """
                        UPDATE users
                        SET follower_count = follower_count + 1,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = :userId
                    """;

        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);

        try {
            int rowsAffected = namedParameterJdbcTemplate.update(sql, params);
            return rowsAffected > 0;
        } catch (Exception e) {
            log.error("Gagal increment follower_count untuk User ID {}: {}", userId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }

    /**
     * Menurunkan jumlah follower_count
     */
    public boolean decrementFollowerCount(Long userId) {
        String sql = """
                        UPDATE users
                        SET follower_count = GREATEST(follower_count - 1, 0),
                            updated_at = CURRENT_TIMESTAMP
                        WHERE id = :userId
                    """;

        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);

        try {
            int rowsAffected = namedParameterJdbcTemplate.update(sql, params);
            return rowsAffected > 0;
        } catch (Exception e) {
            log.error("Gagal decrement follower_count untuk User ID {}: {}", userId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }
    
    /**
     * Menaikkan jumlah bookmark
     */
    public boolean incrementBookmarkCount(Long userId) {
        String sql = """
                    UPDATE users
                    SET bookmark_count = bookmark_count + 1,
                        updated_at = CURRENT_TIMESTAMP
                    WHERE id = :userId
                """;

        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);

        try {
            int rowsAffected = namedParameterJdbcTemplate.update(sql, params);
            return rowsAffected > 0;
        } catch (Exception e) {
            log.error("Gagal increment bookmark_count untuk User ID {}: {}", userId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }

    /**
     * Menurunkan jumlah bookmark_count
     */
    public boolean decrementBookmarkCount(Long userId) {
        String sql = """
                    UPDATE users
                    SET bookmark_count = GREATEST(bookmark_count - 1, 0),
                        updated_at = CURRENT_TIMESTAMP
                    WHERE id = :userId
                """;

        MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);

        try {
            int rowsAffected = namedParameterJdbcTemplate.update(sql, params);
            return rowsAffected > 0;
        } catch (Exception e) {
            log.error("Gagal decrement bookmark_count untuk User ID {}: {}", userId, e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Other Error");
        }
    }
    
}

