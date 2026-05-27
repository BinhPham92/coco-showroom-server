package com.cocoshowroom.server.admin;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cocoshowroom.server.auth.Role;
import com.cocoshowroom.server.auth.User;
import com.cocoshowroom.server.auth.UserRepository;
import com.cocoshowroom.server.order.Order;
import com.cocoshowroom.server.order.OrderRepository;
import com.cocoshowroom.server.order.OrderStatus;
import com.cocoshowroom.server.order.PaymentMethod;
import com.cocoshowroom.server.product.ProductRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminOrderControllerTest {

  @Autowired
  MockMvc mockMvc;
  @Autowired
  OrderRepository orderRepository;
  @Autowired
  ProductRepository productRepository;
  @Autowired
  UserRepository userRepository;

  @BeforeEach
  void setUp() {
    orderRepository.deleteAll();
    productRepository.deleteAll();
    userRepository.deleteAll();
  }

  // ── Auth ─────────────────────────────────────────────────────────────────

  @Test
  void list_noJwt_returns401() throws Exception {
    mockMvc.perform(get("/v1/admin/orders"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void list_customerRole_returns403() throws Exception {
    mockMvc.perform(get("/v1/admin/orders")
            .with(customerJwt()))
        .andExpect(status().isForbidden());
  }

  // ── Empty state ───────────────────────────────────────────────────────────

  @Test
  void list_noOrders_returnsEmptyPage() throws Exception {
    mockMvc.perform(get("/v1/admin/orders")
            .with(staffJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items").isArray())
        .andExpect(jsonPath("$.items.length()").value(0))
        .andExpect(jsonPath("$.nextCursor").doesNotExist())
        .andExpect(jsonPath("$.total").value(0));
  }

  // ── Full listing ──────────────────────────────────────────────────────────

  @Test
  void list_withOrders_returnsAllOrders() throws Exception {
    saveOrder(OrderStatus.PENDING);
    saveOrder(OrderStatus.CONFIRMED);

    mockMvc.perform(get("/v1/admin/orders")
            .with(staffJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(2))
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.nextCursor").doesNotExist());
  }

  // ── Status filter ─────────────────────────────────────────────────────────

  @Test
  void list_statusFilter_returnsOnlyMatchingOrders() throws Exception {
    saveOrder(OrderStatus.PENDING);
    saveOrder(OrderStatus.PENDING);
    saveOrder(OrderStatus.CONFIRMED);

    mockMvc.perform(get("/v1/admin/orders?status=PENDING")
            .with(staffJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(2))
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.items[0].status").value("PENDING"))
        .andExpect(jsonPath("$.items[1].status").value("PENDING"));
  }

  // ── Pagination ────────────────────────────────────────────────────────────

  @Test
  void list_limitOne_returnsNextCursor() throws Exception {
    saveOrder(OrderStatus.PENDING);
    saveOrder(OrderStatus.PENDING);

    String body = mockMvc.perform(get("/v1/admin/orders?limit=1")
            .with(staffJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.nextCursor").isNotEmpty())
        .andReturn().getResponse().getContentAsString();

    // Follow the cursor to get the second page
    String cursor = new com.fasterxml.jackson.databind.ObjectMapper()
        .readTree(body).get("nextCursor").asText();

    mockMvc.perform(get("/v1/admin/orders?limit=1&cursor=" + cursor)
            .with(staffJwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.nextCursor").doesNotExist());
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private void saveOrder(OrderStatus status) {
    Order order = new Order();
    order.setShippingName("Test User");
    order.setShippingPhone("0901234567");
    order.setShippingAddress("123 Test St");
    order.setShippingDistrict("Quận 1");
    order.setShippingCity("TP.HCM");
    order.setContactEmail("test@example.com");
    order.setLocale("vi");
    order.setPaymentMethod(PaymentMethod.COD);
    order.setTotalVnd(100_000L);
    order.setStatus(status);
    orderRepository.save(order);
  }

  private org.springframework.test.web.servlet.request.RequestPostProcessor staffJwt() {
    return jwt().jwt(j -> j
        .subject(UUID.randomUUID().toString())
        .claim("role", "STAFF")
    ).authorities(new SimpleGrantedAuthority("ROLE_STAFF"));
  }

  private org.springframework.test.web.servlet.request.RequestPostProcessor customerJwt() {
    User user = new User();
    user.setRole(Role.CUSTOMER);
    UUID userId = userRepository.save(user).getId();
    return jwt().jwt(j -> j
        .subject(userId.toString())
        .claim("role", "CUSTOMER")
    ).authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
  }
}
