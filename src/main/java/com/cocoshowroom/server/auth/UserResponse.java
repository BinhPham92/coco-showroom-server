package com.cocoshowroom.server.auth;

import java.util.UUID;

public record UserResponse(UUID id, String email, String name, String phone, String role) {

    static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getRole().name()
        );
    }
}
