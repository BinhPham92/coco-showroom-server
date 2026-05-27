package com.cocoshowroom.server.admin;

import com.cocoshowroom.server.order.OrderResponse;
import com.cocoshowroom.server.order.OrderStatus;
import com.cocoshowroom.server.shared.AdminPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Staff-only order listing.
 *
 * <p>Access to all {@code /v1/admin/**} paths is restricted to {@code ROLE_STAFF}
 * by a blanket rule in {@link com.cocoshowroom.server.config.SecurityConfig}.
 *
 * <p>Example requests:
 * <pre>
 *   GET /v1/admin/orders                      – first page of all orders
 *   GET /v1/admin/orders?status=PENDING       – pending orders only
 *   GET /v1/admin/orders?cursor=&lt;token&gt;       – next page
 *   GET /v1/admin/orders?limit=50             – up to 50 items per page (max 100)
 * </pre>
 */
@RestController
@RequestMapping("/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    public AdminPageResponse<OrderResponse> list(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return adminOrderService.list(status, cursor, limit);
    }
}
