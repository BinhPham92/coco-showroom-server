package com.cocoshowroom.server.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body for {@code POST /v1/auth/change-password}.
 *
 * <p>Requiring the current password prevents an attacker who has stolen a
 * valid JWT from locking the real user out of their account.
 */
public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8, max = 100) String newPassword
) {}
