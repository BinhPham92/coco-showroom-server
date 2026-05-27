package com.cocoshowroom.server.admin;

import com.cocoshowroom.server.review.ReviewResponse;
import com.cocoshowroom.server.review.ReviewService;
import com.cocoshowroom.server.review.ReviewStatus;
import com.cocoshowroom.server.review.UpdateReviewStatusRequest;
import com.cocoshowroom.server.shared.AdminPageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Staff-only review listing / moderation queue.
 *
 * <p>Access to all {@code /v1/admin/**} paths is restricted to {@code ROLE_STAFF}
 * by a blanket rule in {@link com.cocoshowroom.server.config.SecurityConfig}.
 *
 * <p>Example requests:
 * <pre>
 *   GET /v1/admin/reviews                      – all reviews, newest first
 *   GET /v1/admin/reviews?status=PENDING       – moderation queue
 *   GET /v1/admin/reviews?status=APPROVED      – approved reviews
 *   GET /v1/admin/reviews?cursor=&lt;token&gt;       – next page
 * </pre>
 */
@RestController
@RequestMapping("/v1/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final AdminReviewService adminReviewService;
    private final ReviewService reviewService;

    @GetMapping
    public AdminPageResponse<ReviewResponse> list(
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return adminReviewService.list(status, cursor, limit);
    }

    /**
     * Moderates a review by ID without requiring the product slug.
     * Convenience endpoint for the admin dashboard; delegates to the same
     * {@link ReviewService#moderateReview} used by the product-scoped endpoint.
     *
     * <p>Access is restricted to {@code ROLE_STAFF} by the blanket
     * {@code /v1/admin/**} rule in {@link com.cocoshowroom.server.config.SecurityConfig}.
     */
    @PatchMapping("/{id}/status")
    public ReviewResponse moderateReview(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReviewStatusRequest request
    ) {
        return reviewService.moderateReview(id, request.status());
    }
}
