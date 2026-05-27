package com.cocoshowroom.server.email;

import com.cocoshowroom.server.order.OrderResponse;

/**
 * Published after an order is persisted and its DB transaction committed.
 * Consumed by {@link OrderEmailService} to send the confirmation email.
 */
public record OrderConfirmedEvent(OrderResponse order) {
}
