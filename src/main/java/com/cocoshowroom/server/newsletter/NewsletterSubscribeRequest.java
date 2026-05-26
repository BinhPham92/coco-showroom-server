package com.cocoshowroom.server.newsletter;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body for {@code POST /v1/newsletter}. */
public record NewsletterSubscribeRequest(
        @NotBlank @Email @Size(max = 320) String email
) {}
