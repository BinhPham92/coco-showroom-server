package com.cocoshowroom.server.auth;

import java.util.UUID;

public record UserResponse(UUID id, String email, String name, String phone, String role) {

    /**
     * Builds the response from a {@link User} and its associated email address.
     *
     * <p>Email is passed separately because it lives on {@link UserIdentity} (W8).
     * The JWT already contains the email claim, so callers that have a JWT can
     * read email directly without an extra DB query.
     */
    public static UserResponse from(User user, String email) {
        return new UserResponse(
            user.getId(),
            email,
            user.getName(),
            user.getPhone(),
            user.getRole().name()
        );
    }
}
