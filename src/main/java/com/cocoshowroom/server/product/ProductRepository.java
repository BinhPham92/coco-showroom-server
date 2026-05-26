package com.cocoshowroom.server.product;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /**
     * Cursor-based pagination ordered by createdAt DESC.
     * Cursor is the createdAt of the last item on the previous page.
     * Passing null for any filter param disables that filter.
     */
    @Query("""
        SELECT p FROM Product p
        WHERE (:category IS NULL OR p.category = :category)
          AND (:grade    IS NULL OR p.grade    = :grade)
          AND (:featured IS NULL OR p.featured = :featured)
          AND (:cursor   IS NULL OR p.createdAt < :cursor)
        ORDER BY p.createdAt DESC
        """)
    List<Product> findPage(
        @Param("category") Category category,
        @Param("grade")    Grade grade,
        @Param("featured") Boolean featured,
        @Param("cursor")   Instant cursor,
        Pageable pageable
    );
}
