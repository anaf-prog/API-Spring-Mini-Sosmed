package com.sosmed.security;

import java.io.IOException;

import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userIdStr;

        // 1. Cek jika header Authorization kosong atau tidak diawali "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Potong string untuk mengambil token JWT saja
        jwt = authHeader.substring(7);
        
        try {
            // 3. Ekstrak User ID (Subject) dari token
            userIdStr = jwtService.extractUserId(jwt);

            // 4. Jika ID ada dan user belum terotentikasi di Context Spring Security
            if (userIdStr != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // loadUserByUsername di sini menerima string userIdStr
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userIdStr);
                
                // Ambil objek User asli untuk divalidasi id-nya
                CustomUserDetails customUserDetails = (CustomUserDetails) userDetails;

                // 5. Validasi kecocokan token dengan ID user di database
                if (jwtService.isTokenValid(jwt, customUserDetails.getId())) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // 6. Set user ke dalam Security Context (User resmi dianggap "Login")
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // Jika token tidak valid / expired, biarkan request berlanjut tanpa otentikasi
            // Nantinya Spring Security otomatis menolak di SecurityConfig
            log.error("Token tidak valid {}: ", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }
    
}
