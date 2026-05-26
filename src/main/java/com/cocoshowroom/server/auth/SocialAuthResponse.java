package com.cocoshowroom.server.auth;

import java.util.UUID;

/**
 * Response from {@code POST /v1/auth/social}.
 *
 * @param token     The signed HS256 JWT the client should store in the HttpOnly
 *                  session cookie (BFF handles this).
 * @param userId    UUID of the user record.
 * @param email     Email address sourced from the OAuth provider.
 * @param role      Role name (e.g. "CUSTOMER", "STAFF").
 * @param isNewUser {@code true} when the account was created during this sign-in.
 *                  The BFF can use this to show an onboarding screen.
 */
public record SocialAuthResponse(
    String token,
    UUID userId,
    String email,
    String role,
    boolean isNewUser
) {}
