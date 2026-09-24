package com.vehicletelematics.backend.users.api.dto;

import com.vehicletelematics.backend.users.domain.User;
import com.vehicletelematics.backend.users.domain.UserRole;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        UserRole role,
        boolean enabled,
        Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt());
    }
}
