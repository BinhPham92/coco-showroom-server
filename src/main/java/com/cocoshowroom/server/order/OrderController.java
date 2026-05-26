package com.cocoshowroom.server.order;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable UUID id) {
        return orderService.getOrder(id);
    }
}
