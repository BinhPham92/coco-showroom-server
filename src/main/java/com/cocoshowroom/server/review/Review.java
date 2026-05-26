package com.cocoshowroom.server.review;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

// Review entity

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** FK to products.id. Stored as a plain UUID column — same pattern as Order.userId. */
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    /** FK to users.id. Null for guest reviews. */
    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, length = 200)
    private String authorName;

    /** Star rating 1–5. */
    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewStatus status = ReviewStatus.PENDING;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
