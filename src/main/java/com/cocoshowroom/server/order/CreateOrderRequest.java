package com.cocoshowroom.server.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

/**
 * Body for {@code POST /v1/orders}.
 *
 * <p>Shipping validation mirrors the client-side Zod schema in
 * {@code features/checkout/lib/schema.ts}.
 * Items carry only sku + qty — the server snapshots price and name from
 * the products table at creation time.
 */
public record CreateOrderRequest(

        @NotEmpty @Valid List<@Valid OrderLineItem> items,

        @NotBlank @Size(min = 2) String shippingName,
        @NotBlank @Pattern(regexp = "^(0|\\+84)[0-9]{9}$") String shippingPhone,
        @NotBlank @Size(min = 5) String shippingAddress,
        @NotBlank @Size(min = 2) String shippingDistrict,
        @NotBlank @Size(min = 2) String shippingCity,
        String shippingNote,

        // Email address for the order confirmation — required at checkout.
        @NotBlank @Email String contactEmail,

        // Locale for the order-confirmation email — "vi" (default) or "en".
        @Pattern(regexp = "^(vi|en)$", message = "must be 'vi' or 'en'") String locale,

        @NotNull PaymentMethod paymentMethod

) {
    public record OrderLineItem(
            @NotBlank String sku,
            @Min(1) int qty
    ) {}
}
