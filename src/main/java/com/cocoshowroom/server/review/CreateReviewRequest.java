package com.cocoshowroom.server.review;

import jakarta.validation.constraints.*;

/** Body for {@code POST /v1/products/:slug/reviews}. */
public record CreateReviewRequest(
        @NotBlank @Size(max = 200) String authorName,
        @Min(1) @Max(5) int rating,
        @Size(max = 2000) String body
) {}
