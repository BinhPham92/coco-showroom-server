package com.cocoshowroom.server.order;

import com.cocoshowroom.server.email.OrderConfirmedEvent;
import com.cocoshowroom.server.product.Product;
import com.cocoshowroom.server.product.ProductRepository;
import com.cocoshowroom.server.shared.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository            orderRepository;
    private final ProductRepository          productRepository;
    private final ApplicationEventPublisher  eventPublisher;

    /**
     * Creates a new order from the checkout form submission.
     *
     * <p>Each line item's price and name are snapshotted from the current product
     * record so the order is self-contained even if the catalogue changes later.
     *
     * <p>If {@code idempotencyKey} is provided and an order with that key already
     * exists, the existing order is returned immediately — preventing duplicate orders
     * on network retry.
     *
     * @param userId         null for guest checkouts
     * @param idempotencyKey client-supplied deduplication key (from {@code X-Idempotency-Key}
     *                       header); null if the header was absent
     */
    @Transactional
    public OrderResponse createOrder(UUID userId, CreateOrderRequest req, String idempotencyKey) {
        // Idempotency check — return existing order on retry.
        if (idempotencyKey != null) {
            Order existing = orderRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
            if (existing != null) {
                return OrderResponse.from(existing);
            }
        }

        // Batch-fetch all required products in one query to avoid N+1.
        List<String> slugs = req.items().stream()
                .map(CreateOrderRequest.OrderLineItem::sku)
                .toList();
        Map<String, Product> productsBySlug = productRepository.findAllBySlugIn(slugs)
                .stream()
                .collect(Collectors.toMap(Product::getSlug, Function.identity()));

        // Validate that every requested SKU exists.
        for (String slug : slugs) {
            if (!productsBySlug.containsKey(slug)) {
                throw new NotFoundException("Product not found: " + slug);
            }
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setIdempotencyKey(idempotencyKey);
        order.setShippingName(req.shippingName());
        order.setShippingPhone(req.shippingPhone());
        order.setShippingAddress(req.shippingAddress());
        order.setShippingDistrict(req.shippingDistrict());
        order.setShippingCity(req.shippingCity());
        order.setShippingNote(req.shippingNote());
        order.setContactEmail(req.contactEmail());
        order.setLocale(req.locale() != null ? req.locale() : "vi");
        order.setPaymentMethod(req.paymentMethod());

        long total = 0;
        for (CreateOrderRequest.OrderLineItem line : req.items()) {
            Product product = productsBySlug.get(line.sku());

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductId(product.getId());
            item.setSku(product.getSlug());
            item.setNameVi(product.getNameVi());
            item.setNameEn(product.getNameEn());
            item.setQty(line.qty());
            item.setPriceVnd(product.getPriceVnd());

            order.getItems().add(item);
            total += product.getPriceVnd() * line.qty();
        }

        order.setTotalVnd(total);
        // saveAndFlush so @CreationTimestamp is populated before we build the response
        OrderResponse saved = OrderResponse.from(orderRepository.saveAndFlush(order));
        // Publish after flush — the @TransactionalEventListener fires once the outer
        // transaction commits, ensuring the email goes out only for persisted orders.
        eventPublisher.publishEvent(new OrderConfirmedEvent(saved));
        return saved;
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        return OrderResponse.from(order);
    }

    /** Returns all orders belonging to the authenticated user, newest first. */
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(UUID userId) {
        return orderRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    /**
     * Updates order status. Restricted to STAFF — enforced via
     * {@code @PreAuthorize} on the controller method.
     */
    @Transactional
    public OrderResponse updateStatus(UUID orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        order.setStatus(newStatus);
        return OrderResponse.from(orderRepository.saveAndFlush(order));
    }
}
