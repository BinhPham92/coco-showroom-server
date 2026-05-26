package com.cocoshowroom.server.order;

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
 * Order endpoints.
 *
 * <p>{@code POST /v1/orders} is public — guest checkout is supported.
 * If a valid JWT is present the order is associated with that user;
 * otherwise {@code userId} is null (guest order).
 *
 * <p>{@code GET /v1/orders/:id} is also public so the confirm page can
 * display the order without requiring the user to be signed in.
 */
@RestController
@RequestMapping("/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(
            @AuthenticationPrincipal Jwt jwt,   // null for guest requests
            @Valid @RequestBody CreateOrderRequest request
    ) {
        UUID userId = jwt != null ? UUID.fromString(jwt.getSubject()) : null;
        return orderService.createOrder(userId, request);
    }

    /** Returns the order by ID. Public so the confirm page works for guests. */
    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable UUID id) {
        return orderService.getOrder(id);
    }

    /**
     * Returns all orders belonging to the authenticated user, newest first.
     * Used by the account order-history page.
     */
    @GetMapping
    public List<OrderResponse> getMyOrders(@AuthenticationPrincipal Jwt jwt) {
        return orderService.getMyOrders(UUID.fromString(jwt.getSubject()));
    }

    /**
     * Updates an order's status. STAFF only.
     * Example: mark as CONFIRMED after reviewing, SHIPPED after dispatch.
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('STAFF')")
    public OrderResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        return orderService.updateStatus(id, request.status());
    }
}
