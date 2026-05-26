package com.cocoshowroom.server.order;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        OrderStatus status,
        List<OrderItemResponse> items,
        String shippingName,
        String shippingPhone,
        String shippingAddress,
        String shippingDistrict,
        String shippingCity,
        String shippingNote,
        PaymentMethod paymentMethod,
        long totalVnd,
        Instant createdAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getShippingName(),
                order.getShippingPhone(),
                order.getShippingAddress(),
                order.getShippingDistrict(),
                order.getShippingCity(),
                order.getShippingNote(),
                order.getPaymentMethod(),
                order.getTotalVnd(),
                order.getCreatedAt()
        );
    }
}
