package com.cocoshowroom.server.order;

public record OrderItemResponse(
        String sku,
        String nameVi,
        String nameEn,
        int qty,
        long priceVnd
) {
    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getSku(),
                item.getNameVi(),
                item.getNameEn(),
                item.getQty(),
                item.getPriceVnd()
        );
    }
}
