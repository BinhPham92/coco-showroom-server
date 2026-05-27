package com.cocoshowroom.server.review;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    /** Public listing — only APPROVED reviews. */
    List<Review> findAllByProductIdAndStatusOrderByCreatedAtDesc(UUID productId, ReviewStatus status);

    boolean existsByProductIdAndUserId(UUID productId, UUID userId);

    /**
     * Admin paginated listing ordered by {@code (createdAt DESC, id DESC)}.
     *
     * <p>The composite sort key prevents ties when multiple reviews are created in
     * the same millisecond. Pass {@code null} for all three optional parameters to
     * fetch the first page.
     */
    @Query("""
        SELECT r FROM Review r
        WHERE (:status IS NULL OR r.status = :status)
          AND (:before IS NULL
                OR r.createdAt < :before
                OR (r.createdAt = :before AND r.id < :lastId))
        ORDER BY r.createdAt DESC, r.id DESC
        """)
    List<Review> findForAdmin(
            @Param("status") ReviewStatus status,
            @Param("before") Instant before,
            @Param("lastId") java.util.UUID lastId,
            Pageable pageable
    );

    /** Total count matching the optional status filter. */
    @Query("SELECT COUNT(r) FROM Review r WHERE (:status IS NULL OR r.status = :status)")
    long countForAdmin(@Param("status") ReviewStatus status);
}
