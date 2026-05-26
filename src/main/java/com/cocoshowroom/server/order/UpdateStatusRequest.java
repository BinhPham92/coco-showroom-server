package com.cocoshowroom.server.order;

import jakarta.validation.constraints.NotNull;

/** Body for {@code PATCH /v1/orders/:id/status}. STAFF only. */
public record UpdateStatusRequest(@NotNull OrderStatus status) {}
