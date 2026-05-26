package com.cocoshowroom.server.cart;

import com.cocoshowroom.server.product.Product;

/**
 * Wire shape returned to the Next.js BFF — mirrors the client's {@code CartItem} type:
 * <pre>
 * { sku: string; qty: number; addedAt: number; nameVi?: string; nameEn?: string; priceVnd?: number }
 * </pre>
 * {@code sku} is the product slug; {@code addedAt} is epoch-milliseconds.
 */
public record CartItemResponse(
        String sku,
        int qty,
        long addedAt,
        String nameVi,
        String nameEn,
        long priceVnd
) {
    public static CartItemResponse from(CartItem item) {
        Product p = item.getProduct();
        return new CartItemResponse(
                p.getSlug(),
                item.getQty(),
                item.getAddedAt().toEpochMilli(),
                p.getNameVi(),
                p.getNameEn(),
                p.getPriceVnd()
        );
    }
}
