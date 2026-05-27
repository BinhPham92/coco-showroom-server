package com.cocoshowroom.server.admin;

import com.cocoshowroom.server.order.Order;
import com.cocoshowroom.server.order.OrderRepository;
import com.cocoshowroom.server.order.OrderResponse;
import com.cocoshowroom.server.order.OrderStatus;
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
class AdminOrderService {

    private static final int MAX_LIMIT = 100;

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public AdminPageResponse<OrderResponse> list(OrderStatus status, String cursorStr, int limit) {
        int clampedLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);

        Instant before = null;
        UUID lastId = null;
        if (cursorStr != null) {
            String[] parts = decodeCursor(cursorStr);
            before = Instant.parse(parts[0]);
            lastId = UUID.fromString(parts[1]);
        }

        // Fetch one extra row to determine whether a next page exists
        List<Order> rows = orderRepository.findForAdmin(
                status, before, lastId, PageRequest.of(0, clampedLimit + 1));

        boolean hasNext = rows.size() > clampedLimit;
        List<Order> page = hasNext ? rows.subList(0, clampedLimit) : rows;
        Order last = page.isEmpty() ? null : page.getLast();
        String nextCursor = (hasNext && last != null)
                ? encodeCursor(last.getCreatedAt(), last.getId()) : null;

        long total = orderRepository.countForAdmin(status);

        return new AdminPageResponse<>(
                page.stream().map(OrderResponse::from).toList(),
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
