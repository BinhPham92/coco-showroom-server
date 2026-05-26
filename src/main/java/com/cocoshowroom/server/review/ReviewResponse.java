package com.cocoshowroom.server.review;

import java.util.UUID;

public record ReviewResponse(
        UUID id,
        String authorName,
        int rating,
        String body,
        String status,
        long createdAt   // epoch-ms, consistent with other timestamp fields
) {
    static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getAuthorName(),
                review.getRating(),
                review.getBody(),
                review.getStatus().name(),
                review.getCreatedAt().toEpochMilli()
        );
    }
}
