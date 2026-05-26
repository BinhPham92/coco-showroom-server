package com.cocoshowroom.server.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code POST /v1/auth/social}.
 *
 * @param provider The OAuth provider (GOOGLE or FACEBOOK).
 * @param token    The raw token from the client SDK:
 *                 {@code id_token} for Google, {@code access_token} for Facebook.
 */
public record SocialAuthRequest(
    @NotNull OAuthProvider provider,
    @NotBlank String token
) {}
