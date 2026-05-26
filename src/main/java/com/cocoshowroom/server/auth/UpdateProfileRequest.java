package com.cocoshowroom.server.auth;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body for {@code PATCH /v1/auth/me}.
 *
 * <p>Semantics:
 * <ul>
 *   <li>Field absent / {@code null} → leave unchanged.
 *   <li>Field {@code ""} (empty string) → clear to {@code null}.
 *   <li>Field non-empty → validate and set.
 * </ul>
 */
public record UpdateProfileRequest(
        @Size(max = 200) String name,
        @Pattern(regexp = "^$|^(0|\\+84)[0-9]{9}$",
                 message = "must be a valid Vietnamese phone number or empty string to clear") String phone
) {}
