package com.cocoshowroom.server.review;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Review endpoints nested under the product slug.
 *
 * <p>{@code GET /v1/products/:slug/reviews} is public.
 * {@code POST /v1/products/:slug/reviews} accepts an optional JWT — guest
 * reviews are allowed; authenticated users may only review a product once.
 *
 * <p><b>TODO (post-v1):</b> add IP-based rate limiting (Bucket4j) on the
 * POST endpoint to throttle guest review spam. The moderation queue is the
 * first line of defence for now.
 */
@RestController
@RequestMapping("/v1/products/{slug}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public List<ReviewResponse> getReviews(@PathVariable String slug) {
        return reviewService.getReviews(slug);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse createReview(
            @PathVariable String slug,
            @AuthenticationPrincipal Jwt jwt,   // null for guest requests
            @Valid @RequestBody CreateReviewRequest request
    ) {
        UUID userId = jwt != null ? UUID.fromString(jwt.getSubject()) : null;
        return reviewService.createReview(slug, userId, request);
    }

    /**
     * Updates a review's moderation status. STAFF only.
     * Example: APPROVE a legitimate review, REJECT spam.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('STAFF')")
    public ReviewResponse moderateReview(
            @PathVariable String slug,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReviewStatusRequest request
    ) {
        return reviewService.moderateReview(id, request.status());
    }
}
