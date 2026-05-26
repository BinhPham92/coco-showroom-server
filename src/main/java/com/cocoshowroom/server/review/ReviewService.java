package com.cocoshowroom.server.review;

import com.cocoshowroom.server.auth.User;
import com.cocoshowroom.server.auth.UserRepository;
import com.cocoshowroom.server.product.Product;
import com.cocoshowroom.server.product.ProductRepository;
import com.cocoshowroom.server.shared.ConflictException;
import com.cocoshowroom.server.shared.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    /** Returns APPROVED reviews for the product, newest first. Pending/rejected are hidden. */
    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviews(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Product not found: " + slug));
        return reviewRepository
                .findAllByProductIdAndStatusOrderByCreatedAtDesc(product.getId(), ReviewStatus.APPROVED)
                .stream()
                .map(ReviewResponse::from)
                .toList();
    }

    /**
     * Updates a review's moderation status. Restricted to STAFF — enforced via
     * {@code @PreAuthorize} on the controller method.
     */
    @Transactional
    public ReviewResponse moderateReview(UUID reviewId, ReviewStatus newStatus) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NotFoundException("Review not found"));
        review.setStatus(newStatus);
        return ReviewResponse.from(reviewRepository.saveAndFlush(review));
    }

    /**
     * Creates a new review for the product.
     *
     * <p>Guest reviews (userId == null) are always accepted.
     * Authenticated users may only review a product once.
     *
     * <p>When the caller is authenticated and has a profile name set, that name
     * is used instead of {@code req.authorName()} to prevent impersonation.
     * If the user has no profile name yet, the request's {@code authorName} is
     * used as a fallback.
     */
    @Transactional
    public ReviewResponse createReview(String slug, UUID userId, CreateReviewRequest req) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Product not found: " + slug));

        if (userId != null && reviewRepository.existsByProductIdAndUserId(product.getId(), userId)) {
            throw new ConflictException("You have already reviewed this product");
        }

        // Resolve the display name: authenticated users with a profile name cannot
        // impersonate someone else — the server always uses the name on their account.
        String resolvedName = req.authorName();
        if (userId != null) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NotFoundException("User not found"));
            if (user.getName() != null && !user.getName().isBlank()) {
                resolvedName = user.getName();
            }
        }

        Review review = new Review();
        review.setProductId(product.getId());
        review.setUserId(userId);
        review.setAuthorName(resolvedName);
        review.setRating(req.rating());
        review.setBody(req.body());

        return ReviewResponse.from(reviewRepository.saveAndFlush(review));
    }
}
