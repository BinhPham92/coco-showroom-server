package com.cocoshowroom.server.order;

import com.cocoshowroom.server.product.Product;
import com.cocoshowroom.server.product.ProductRepository;
import com.cocoshowroom.server.shared.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    /**
     * Creates a new order from the checkout form submission.
     *
     * <p>Each line item's price and name are snapshotted from the current product
     * record so the order is self-contained even if the catalogue changes later.
     *
     * @param userId null for guest checkouts
     */
    @Transactional
    public OrderResponse createOrder(UUID userId, CreateOrderRequest req) {
        Order order = new Order();
        order.setUserId(userId);
        order.setShippingName(req.shippingName());
        order.setShippingPhone(req.shippingPhone());
        order.setShippingAddress(req.shippingAddress());
        order.setShippingDistrict(req.shippingDistrict());
        order.setShippingCity(req.shippingCity());
        order.setShippingNote(req.shippingNote());
        order.setPaymentMethod(req.paymentMethod());

        long total = 0;
        for (CreateOrderRequest.OrderLineItem line : req.items()) {
            Product product = productRepository.findBySlug(line.sku())
                    .orElseThrow(() -> new NotFoundException("Product not found: " + line.sku()));

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
        return OrderResponse.from(orderRepository.saveAndFlush(order));
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
