package com.cocoshowroom.server.review;

import jakarta.validation.constraints.NotNull;

/** Body for {@code PATCH /v1/products/:slug/reviews/:id/status}. STAFF only. */
public record UpdateReviewStatusRequest(@NotNull ReviewStatus status) {}
