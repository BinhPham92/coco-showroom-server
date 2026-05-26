package com.cocoshowroom.server.cart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Body for {@code POST /v1/cart/reconcile}.
 *
 * <p>The client sends its local Zustand cart on sign-in. The server merges these items
 * with any previously persisted cart, returning the unified result.
 */
public record ReconcileRequest(
        @NotNull @Valid List<@Valid ReconcileItem> items
) {
    public record ReconcileItem(
            /** Product slug — maps to {@code CartItem.sku} on the client. */
            @NotBlank String sku,
            @Min(1) int qty,
            /** Epoch-milliseconds — matches client {@code CartItem.addedAt: number}. */
            long addedAt
    ) {}
}
