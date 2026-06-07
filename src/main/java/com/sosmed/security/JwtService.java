package com.sosmed.security;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {
    
    @Value("${secret-key}")
    private String SECRET_KEY;

    @Value("${expiration-jwt}")
    private Long EXPIRATION_TIME;

    @Value("${jwt-key-id}")
    private String KEY_ID;

    @Value("${jwt-issuer}")
    private String ISSUER;

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    /**
     * Generate token menggunakan ID User sebagai Subject
     */
    public String generateToken(Long userId) {
        Instant now = Instant.now();
        
        // Mengonversi EXPIRATION_TIME dari milidetik ke detik untuk Instant
        Instant expiresAt = now.plusMillis(EXPIRATION_TIME);

        // 1. Definisikan Header JWS secara eksplisit menggunakan HS256 agar lolos seleksi kunci
        JwsHeader jwsHeader = JwsHeader.with(MacAlgorithm.HS256)
            .keyId(KEY_ID)
            .build();

        // 2. Definisikan Isian (Claims) dari JWT
        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer(ISSUER)
            .issuedAt(now)
            .expiresAt(expiresAt)
            .subject(String.valueOf(userId)) // ID diubah ke String untuk disimpan di Subject JWT
            .build();

        // 3. Gabungkan Header dan Claims ke dalam parameter enkripsi encoder
        JwtEncoderParameters parameters = JwtEncoderParameters.from(jwsHeader, claims);

        return this.jwtEncoder.encode(parameters).getTokenValue();
    }

    /**
     * Ambil ID User (Subject) dari token
     */
    public String extractUserId(String token) {
        try {
            Jwt jwt = this.jwtDecoder.decode(token);
            return jwt.getSubject();
        } catch (JwtException e) {
            log.error("Error JWT : {}", e.getMessage());
            return null; // Mengembalikan null jika token tidak valid atau tidak bisa di-parse
        }
    }

    /**
     * Validasi apakah token cocok dengan ID user dan belum expired
     */
    public boolean isTokenValid(String token, Long userId) {
        try {
            Jwt jwt = this.jwtDecoder.decode(token);
            String extractedId = jwt.getSubject();
            Instant expiresAt = jwt.getExpiresAt();

            // Memeriksa kesesuaian ID dan memastikan token belum kadaluwarsa
            boolean isIdMatch = extractedId.equals(String.valueOf(userId));
            boolean isNotExpired = expiresAt != null && expiresAt.isAfter(Instant.now());

            return isIdMatch && isNotExpired;
        } catch (JwtException e) {
            log.error("Token Error : {}", e.getMessage());
            return false; // Jika token expired atau rusak, otomatis dianggap tidak valid
        }
    }
}
