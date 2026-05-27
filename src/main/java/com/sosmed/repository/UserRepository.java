package com.sosmed.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Repository;

import com.sosmed.model.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

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
                        RETURNING id, fullname, username, email, password, bio, image, image_id, following_count, follower_count, post_count, created_at, updated_at
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
                new BeanPropertyRowMapper<>(User.class));

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
                new BeanPropertyRowMapper<>(User.class));
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

            return namedParameterJdbcTemplate.query(sql, params, new BeanPropertyRowMapper<>(User.class))
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

        return namedParameterJdbcTemplate.query(sql, params, new BeanPropertyRowMapper<>(User.class));
    }
    
}

