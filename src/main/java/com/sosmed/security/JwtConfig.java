package com.sosmed.security;

import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

/**
 * Konfigurasi keamanan untuk JSON Web Token (JWT).
 * Kelas ini bertanggung jawab untuk menginstansiasi komponen enkripsi (Encoder)
 * dan dekripsi (Decoder) JWT yang mematuhi standar OAuth2 Resource Server.
 * Konfigurasi ini juga dioptimalkan agar kompatibel dengan kompilasi GraalVM Native Image.
 */
@Configuration
public class JwtConfig {
    
    @Value("${secret-key}")
    private String SECRET_KEY;

    @Value("${jwt-key-id}")
    private String KEY_ID;

    /**
     * Membuat bean JwtDecoder untuk mendekripsi dan memverifikasi token JWT yang masuk.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        byte[] keyBytes = Base64.getDecoder().decode(SECRET_KEY);
        SecretKey secretKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, "AES");
        return NimbusJwtDecoder.withSecretKey(secretKey).build();
    }

    /**
     * Membuat bean JwtEncoder untuk menandatangani (signing) dan menerbitkan token JWT baru.
     * Kunci simetris dibungkus ke dalam struktur JWK (JSON Web Key) lengkap dengan algoritma
     */
    @Bean
    public JwtEncoder jwtEncoder() {
        byte[] keyBytes = Base64.getDecoder().decode(SECRET_KEY);

        // Membangun JSON Web Key (JWK)
        OctetSequenceKey jwk = new OctetSequenceKey.Builder(keyBytes)
            .algorithm(JWSAlgorithm.HS256)
            .keyID(KEY_ID)
            .build();

        JWKSource<SecurityContext> jwks = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwks);
    }
}
