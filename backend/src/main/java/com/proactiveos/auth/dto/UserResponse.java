package com.proactiveos.auth.dto;

import java.time.Instant;

import com.proactiveos.auth.entity.User;

public record UserResponse(Long id, String email, Instant createdAt, Instant updatedAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getCreatedAt(), user.getUpdatedAt());
    }
}