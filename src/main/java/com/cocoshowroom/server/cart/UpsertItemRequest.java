package com.cocoshowroom.server.cart;

import jakarta.validation.constraints.Min;

/** Body for {@code PUT /v1/cart/items/{slug}}. */
public record UpsertItemRequest(@Min(1) int qty) {}
