package com.cocoshowroom.server.order;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Admin paginated listing ordered by {@code (createdAt DESC, id DESC)}.
     *
     * <p>The composite sort key prevents ties when multiple orders are created in
     * the same millisecond. Pass {@code null} for all three optional parameters to
     * fetch the first page without any filter or cursor.
     *
     * <p>Cursor condition: rows that come strictly after {@code (before, lastId)}
     * in the {@code (createdAt DESC, id DESC)} ordering.
     */
    @Query("""
        SELECT o FROM Order o
        WHERE (:status IS NULL OR o.status = :status)
          AND (:before IS NULL
                OR o.createdAt < :before
                OR (o.createdAt = :before AND o.id < :lastId))
        ORDER BY o.createdAt DESC, o.id DESC
        """)
    List<Order> findForAdmin(
            @Param("status") OrderStatus status,
            @Param("before") Instant before,
            @Param("lastId") java.util.UUID lastId,
            Pageable pageable
    );

    /** Total count matching the optional status filter. */
    @Query("SELECT COUNT(o) FROM Order o WHERE (:status IS NULL OR o.status = :status)")
    long countForAdmin(@Param("status") OrderStatus status);
}
