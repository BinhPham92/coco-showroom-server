package com.cocoshowroom.server.contact;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ContactRepository extends JpaRepository<ContactSubmission, UUID> {

    /**
     * Admin paginated listing ordered by {@code (createdAt DESC, id DESC)}.
     *
     * <p>The composite sort key prevents ties when multiple submissions arrive in
     * the same millisecond. Pass {@code null} for both parameters to fetch the first page.
     */
    @Query("""
        SELECT c FROM ContactSubmission c
        WHERE (:before IS NULL
                OR c.createdAt < :before
                OR (c.createdAt = :before AND c.id < :lastId))
        ORDER BY c.createdAt DESC, c.id DESC
        """)
    List<ContactSubmission> findForAdmin(
            @Param("before") Instant before,
            @Param("lastId") java.util.UUID lastId,
            Pageable pageable
    );
}
