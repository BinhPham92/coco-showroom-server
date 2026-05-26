package com.cocoshowroom.server.cart;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Cart endpoints — all require a valid JWT (CUSTOMER or STAFF).
 *
 * <p>The Next.js BFF forwards the user's session JWT in the {@code Authorization} header.
 * The user identity is extracted from {@code jwt.subject} (the userId UUID).
 */
@RestController
@RequestMapping("/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /** Returns the authenticated user's current server-side cart. */
    @GetMapping
    public List<CartItemResponse> getCart(@AuthenticationPrincipal Jwt jwt) {
        return cartService.getCart(userId(jwt));
    }

    /**
     * Merges the client's local Zustand cart with the server cart and returns
     * the unified result. Called by the BFF immediately after sign-in.
     */
    @PostMapping("/reconcile")
    public List<CartItemResponse> reconcile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ReconcileRequest request
    ) {
        return cartService.reconcile(userId(jwt), request.items());
    }

    /** Creates or updates a single cart item. */
    @PutMapping("/items/{slug}")
    public CartItemResponse upsertItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String slug,
            @Valid @RequestBody UpsertItemRequest request
    ) {
        return cartService.upsertItem(userId(jwt), slug, request.qty());
    }

    /** Removes a single item from the cart. Returns 204 regardless of whether it existed. */
    @DeleteMapping("/items/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String slug
    ) {
        cartService.removeItem(userId(jwt), slug);
    }

    /** Clears the entire cart. */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearCart(@AuthenticationPrincipal Jwt jwt) {
        cartService.clearCart(userId(jwt));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
