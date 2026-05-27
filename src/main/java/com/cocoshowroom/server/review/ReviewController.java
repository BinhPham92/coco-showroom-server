package com.cocoshowroom.server.review;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
 * <p>IP-based rate limiting (5 requests / hour) is enforced by
 * {@link com.cocoshowroom.server.shared.RateLimitingFilter} before this
 * controller is reached. The moderation queue remains the primary content
 * defence.
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
     * Updates a review's moderation status. STAFF only (guarded by SecurityConfig requestMatchers).
     * Example: APPROVE a legitimate review, REJECT spam.
     */
    @PatchMapping("/{id}/status")
    @SuppressWarnings("unused") // slug is required by Spring MVC for path-variable binding
    public ReviewResponse moderateReview(
            @PathVariable String slug,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateReviewStatusRequest request
    ) {
        return reviewService.moderateReview(id, request.status());
    }
}
