package com.cocoshowroom.server.cart;

import com.cocoshowroom.server.auth.User;
import com.cocoshowroom.server.auth.UserRepository;
import com.cocoshowroom.server.product.Product;
import com.cocoshowroom.server.product.ProductRepository;
import com.cocoshowroom.server.shared.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CartItemResponse> getCart(UUID userId) {
        return cartItemRepository.findAllByUserId(userId)
                .stream()
                .map(CartItemResponse::from)
                .toList();
    }

    // ── Reconcile ─────────────────────────────────────────────────────────────

    /**
     * Merges the client's local cart into the server-side cart and returns the result.
     *
     * <p>Strategy — client wins for items present in both:
     * <ul>
     *   <li>Client item exists on server → update qty + addedAt to client values.</li>
     *   <li>Client item is new → add to server cart.</li>
     *   <li>Server-only item → kept unchanged (added from another session/device).</li>
     * </ul>
     * Unknown slugs in the client payload are silently skipped.
     */
    @Transactional
    public List<CartItemResponse> reconcile(UUID userId, List<ReconcileRequest.ReconcileItem> clientItems) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // Index existing server cart by productId for O(1) lookup
        Map<UUID, CartItem> serverCart = cartItemRepository.findAllByUserId(userId)
                .stream()
                .collect(Collectors.toMap(i -> i.getProduct().getId(), i -> i));

        for (ReconcileRequest.ReconcileItem ci : clientItems) {
            productRepository.findBySlug(ci.sku()).ifPresent(product -> {
                CartItem item = serverCart.get(product.getId());
                if (item == null) {
                    item = new CartItem();
                    item.setUser(user);
                    item.setProduct(product);
                }
                item.setQty(ci.qty());
                item.setAddedAt(Instant.ofEpochMilli(ci.addedAt()));
                cartItemRepository.save(item);
            });
        }

        return cartItemRepository.findAllByUserId(userId)
                .stream()
                .map(CartItemResponse::from)
                .toList();
    }

    // ── Upsert single item ────────────────────────────────────────────────────

    @Transactional
    public CartItemResponse upsertItem(UUID userId, String slug, int qty) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Product not found: " + slug));

        CartItem item = cartItemRepository
                .findByUserIdAndProductId(userId, product.getId())
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setUser(user);
                    newItem.setProduct(product);
                    return newItem;
                });

        item.setQty(qty);
        return CartItemResponse.from(cartItemRepository.save(item));
    }

    // ── Remove single item ────────────────────────────────────────────────────

    @Transactional
    public void removeItem(UUID userId, String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Product not found: " + slug));
        cartItemRepository.deleteByUserIdAndProductId(userId, product.getId());
    }

    // ── Clear cart ────────────────────────────────────────────────────────────

    @Transactional
    public void clearCart(UUID userId) {
        cartItemRepository.deleteAllByUserId(userId);
    }
}
