package com.sosmed.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.sosmed.model.User;
import com.sosmed.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserDetailServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String idInString) throws UsernameNotFoundException {
        Long userId = Long.parseLong(idInString);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User dengan ID " + userId + " tidak ditemukan."));

        return new CustomUserDetails(user);        
    }
    
}
