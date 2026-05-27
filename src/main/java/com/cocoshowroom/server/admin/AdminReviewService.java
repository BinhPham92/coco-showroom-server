package com.cocoshowroom.server.admin;

import com.cocoshowroom.server.review.Review;
import com.cocoshowroom.server.review.ReviewRepository;
import com.cocoshowroom.server.review.ReviewResponse;
import com.cocoshowroom.server.review.ReviewStatus;
import com.cocoshowroom.server.shared.AdminPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class AdminReviewService {

    private static final int MAX_LIMIT = 100;

    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public AdminPageResponse<ReviewResponse> list(ReviewStatus status, String cursorStr, int limit) {
        int clampedLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);

        Instant before = null;
        UUID lastId = null;
        if (cursorStr != null) {
            String[] parts = decodeCursor(cursorStr);
            before = Instant.parse(parts[0]);
            lastId = UUID.fromString(parts[1]);
        }

        List<Review> rows = reviewRepository.findForAdmin(
                status, before, lastId, PageRequest.of(0, clampedLimit + 1));

        boolean hasNext = rows.size() > clampedLimit;
        List<Review> page = hasNext ? rows.subList(0, clampedLimit) : rows;
        Review last = page.isEmpty() ? null : page.getLast();
        String nextCursor = (hasNext && last != null)
                ? encodeCursor(last.getCreatedAt(), last.getId()) : null;

        long total = reviewRepository.countForAdmin(status);

        return new AdminPageResponse<>(
                page.stream().map(ReviewResponse::from).toList(),
                nextCursor,
                total
        );
    }

    /**
     * Cursor format: {@code {iso8601timestamp}|{uuid}} → Base64URL.
     * The UUID tiebreaker prevents duplicate rows when records share a millisecond.
     */
    private String encodeCursor(Instant createdAt, UUID id) {
        String combined = createdAt.toString() + "|" + id.toString();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(combined.getBytes(StandardCharsets.UTF_8));
    }

    /** Returns {@code [isoTimestamp, uuid]}. */
    private String[] decodeCursor(String cursor) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            int sep = decoded.lastIndexOf('|');
            return new String[]{decoded.substring(0, sep), decoded.substring(sep + 1)};
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid pagination cursor");
        }
    }
}
