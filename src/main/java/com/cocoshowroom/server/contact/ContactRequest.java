package com.cocoshowroom.server.contact;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for POST /v1/contact.
 * Honeypot, CSRF, and Turnstile fields are stripped by the Next.js BFF before
 * this request reaches the Spring Boot API — only clean business data arrives here.
 */
public record ContactRequest(

    @NotBlank
    @Size(max = 255)
    String name,

    @NotBlank
    @Email
    @Size(max = 320)
    String email,

    @NotBlank
    @Size(max = 500)
    String subject,

    @NotBlank
    @Size(max = 5000)
    String message
) {}
