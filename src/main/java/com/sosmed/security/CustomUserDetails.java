package com.sosmed.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.sosmed.model.User;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {
    
    private final User user; 

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Memberikan default ROLE_USER untuk setiap user yang login
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        // Mengambil password terenkripsi dari objek User untuk divalidasi oleh Spring Security
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        // Mengembalikan username utama
        return user.getUsername();
    }

    // Method tambahan buatan Anaf untuk mengambil data ID atau data lainnya jika dibutuhkan di filter security
    public Long getId() {
        return user.getId();
    }

    public String getEmail() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
    
}
