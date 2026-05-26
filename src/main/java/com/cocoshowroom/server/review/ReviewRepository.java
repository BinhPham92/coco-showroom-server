package com.cocoshowroom.server.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    /** Public listing — only APPROVED reviews. */
    List<Review> findAllByProductIdAndStatusOrderByCreatedAtDesc(UUID productId, ReviewStatus status);

    boolean existsByProductIdAndUserId(UUID productId, UUID userId);
}
