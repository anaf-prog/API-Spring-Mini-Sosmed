package com.sosmed.service;

import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.sosmed.dto.ApiResponse;
import com.sosmed.dto.PagingResponse;
import com.sosmed.dto.follow.FollowResponse;
import com.sosmed.dto.follow.UserFollowResponse;
import com.sosmed.model.User;
import com.sosmed.repository.FollowRepository;
import com.sosmed.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    @Transactional
    public ApiResponse<FollowResponse> toggleFollow(Long followerId, Long followingId) {
        // Validasi tidak boleh mem-follow diri sendiri
        if (followerId.equals(followingId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Anda tidak bisa mem-follow diri sendiri!");
        }

        // Validasi pastikan user target yang mau difollow itu ada di database
        User targetUser = userRepository.findById(followingId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User yang ingin diikuti tidak ditemukan!"));

        boolean alreadyFollowing = followRepository.isFollowing(followerId, followingId);
        String message;

        if (alreadyFollowing) {
            // Jika sudah follow -> Lakukan Unfollow
            followRepository.unfollow(followerId, followingId);
            
            // Update counter di tabel users
            userRepository.decrementFollowingCount(followerId);
            userRepository.decrementFollowerCount(followingId);
            
            message = "Berhasil berhenti mengikuti " + targetUser.getUsername();
        } else {
            // Jika belum follow -> Lakukan Follow
            followRepository.follow(followerId, followingId);
            
            // Update counter di tabel users
            userRepository.incrementFollowingCount(followerId);
            userRepository.incrementFollowerCount(followingId);
            
            message = "Berhasil mengikuti " + targetUser.getUsername();
        }

        UserFollowResponse userFollowResponse = UserFollowResponse.builder()
            .id(targetUser.getId())
            .fullname(targetUser.getFullname())
            .username(targetUser.getUsername())
            .image(targetUser.getImage())
            .build();

        FollowResponse followResponse = FollowResponse.builder()
            .followerId(followerId)
            .followingId(followingId)
            .createdAt(ZonedDateTime.now())
            .updatedAt(ZonedDateTime.now())
            .user(userFollowResponse)
            .build();

        return ApiResponse.<FollowResponse>builder()
            .status(HttpStatus.OK.value())
            .message(message)
            .data(followResponse)
            .build();
    }

    public ApiResponse<List<FollowResponse>> getFollowersList(Long userId, int page, int size) {

        List<FollowResponse> followers = followRepository.findAllFollowers(userId, page, size);
        long totalElements = followRepository.countFollowers(userId);
        int totalPages = (int) Math.ceil((double) totalElements / size);

        PagingResponse paging = PagingResponse.builder()
            .currentPage(page)
            .size(size)
            .totalElements(totalElements)
            .totalPages(totalPages)
            .build();

        return ApiResponse.<List<FollowResponse>>builder()
            .status(HttpStatus.OK.value())
            .message("Berhasil mengambil daftar follower")
            .data(followers)
            .paging(paging)
            .build();
    }

    public ApiResponse<List<FollowResponse>> getFollowingList(Long userId, int page, int size) {

        List<FollowResponse> followers = followRepository.findAllFollowing(userId, page, size);
        long totalElements = followRepository.countFollowing(userId);
        int totalPages = (int) Math.ceil((double) totalElements / size);

        PagingResponse paging = PagingResponse.builder()
            .currentPage(page)
            .size(size)
            .totalElements(totalElements)
            .totalPages(totalPages)
            .build();

        return ApiResponse.<List<FollowResponse>>builder()
            .status(HttpStatus.OK.value())
            .message("Berhasil mengambil daftar following")
            .data(followers)
            .paging(paging)
            .build();
    }
    
}
