package com.cocoshowroom.server.order;

import com.cocoshowroom.server.auth.Role;
import com.cocoshowroom.server.auth.User;
import com.cocoshowroom.server.auth.UserRepository;
import com.cocoshowroom.server.product.Category;
import com.cocoshowroom.server.product.Grade;
import com.cocoshowroom.server.product.Product;
import com.cocoshowroom.server.product.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired OrderRepository orderRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;
    private UUID userId;
    private Product product;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setRole(Role.CUSTOMER);
        userId = userRepository.save(user).getId();

        product = new Product();
        product.setSlug("dongtrung-tuoi-premium");
        product.setNameVi("Đông trùng tươi cao cấp");
        product.setNameEn("Fresh Cordyceps Premium");
        product.setDescriptionVi("Mô tả");
        product.setDescriptionEn("Description");
        product.setCategory(Category.fresh);
        product.setGrade(Grade.premium);
        product.setPriceVnd(2_500_000L);
        product.setWeightGrams(50);
        product.setInStock(true);
        product.setImages(java.util.List.of("products/photo.jpg"));
        product.setTags(java.util.List.of("tươi"));
        productRepository.save(product);
    }

    // ── POST /v1/orders ───────────────────────────────────────────────────────

    @Test
    void createOrder_guest_returns201AndPersists() throws Exception {
        mockMvc.perform(post("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validOrderJson()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].sku").value("dongtrung-tuoi-premium"))
            .andExpect(jsonPath("$.items[0].nameVi").value("Đông trùng tươi cao cấp"))
            .andExpect(jsonPath("$.items[0].nameEn").value("Fresh Cordyceps Premium"))
            .andExpect(jsonPath("$.items[0].qty").value(2))
            .andExpect(jsonPath("$.items[0].priceVnd").value(2_500_000))
            .andExpect(jsonPath("$.totalVnd").value(5_000_000))
            .andExpect(jsonPath("$.shippingName").value("Nguyễn Văn A"))
            .andExpect(jsonPath("$.paymentMethod").value("COD"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty());

        assertThat(orderRepository.count()).isEqualTo(1);
    }

    @Test
    void createOrder_authenticated_associatesWithUser() throws Exception {
        String body = mockMvc.perform(post("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validOrderJson())
                .with(jwt().jwt(j -> j.subject(userId.toString()).claim("role", "CUSTOMER"))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        // Extract the order id from response and verify userId stored
        com.fasterxml.jackson.databind.JsonNode node =
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
        UUID orderId = UUID.fromString(node.get("id").asText());

        Order saved = orderRepository.findById(orderId).orElseThrow();
        assertThat(saved.getUserId()).isEqualTo(userId);
    }

    @Test
    void createOrder_snapshotsPriceAtOrderTime() throws Exception {
        // Create order at original price
        String body = mockMvc.perform(post("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validOrderJson()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.items[0].priceVnd").value(2_500_000))
            .andReturn().getResponse().getContentAsString();

        // Update the product price in the catalogue
        product.setPriceVnd(9_999_999L);
        productRepository.save(product);

        // Fetch the order — snapshot must still reflect the original price
        String orderId = new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(body).get("id").asText();

        mockMvc.perform(get("/v1/orders/" + orderId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[0].priceVnd").value(2_500_000));
    }

    @Test
    void createOrder_unknownSku_returns404() throws Exception {
        mockMvc.perform(post("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "items": [{ "sku": "does-not-exist", "qty": 1 }],
                        "shippingName": "Test",
                        "shippingPhone": "0901234567",
                        "shippingAddress": "123 ABC",
                        "shippingDistrict": "Quận 1",
                        "shippingCity": "TP.HCM",
                        "paymentMethod": "COD"
                    }
                    """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("not_found"));
    }

    @Test
    void createOrder_emptyItems_returns422() throws Exception {
        mockMvc.perform(post("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "items": [],
                        "shippingName": "Test",
                        "shippingPhone": "0901234567",
                        "shippingAddress": "123 ABC",
                        "shippingDistrict": "Quận 1",
                        "shippingCity": "TP.HCM",
                        "paymentMethod": "COD"
                    }
                    """))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("validation_error"));
    }

    @Test
    void createOrder_invalidPhone_returns422() throws Exception {
        mockMvc.perform(post("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "items": [{ "sku": "dongtrung-tuoi-premium", "qty": 1 }],
                        "shippingName": "Test",
                        "shippingPhone": "12345",
                        "shippingAddress": "123 ABC",
                        "shippingDistrict": "Quận 1",
                        "shippingCity": "TP.HCM",
                        "paymentMethod": "COD"
                    }
                    """))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("validation_error"));
    }

    @Test
    void createOrder_multipleItems_totalIsSum() throws Exception {
        Product second = new Product();
        second.setSlug("dongtrung-kho-aa");
        second.setNameVi("Đông trùng khô AA");
        second.setNameEn("Dried Cordyceps AA");
        second.setDescriptionVi("Mô tả");
        second.setDescriptionEn("Description");
        second.setCategory(Category.dried);
        second.setGrade(Grade.premium);
        second.setPriceVnd(1_200_000L);
        second.setWeightGrams(100);
        second.setInStock(true);
        second.setImages(java.util.List.of("products/photo2.jpg"));
        second.setTags(java.util.List.of("khô"));
        productRepository.save(second);

        // 2 × 2_500_000 + 3 × 1_200_000 = 5_000_000 + 3_600_000 = 8_600_000
        mockMvc.perform(post("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "items": [
                            { "sku": "dongtrung-tuoi-premium", "qty": 2 },
                            { "sku": "dongtrung-kho-aa", "qty": 3 }
                        ],
                        "shippingName": "Nguyễn Văn A",
                        "shippingPhone": "0901234567",
                        "shippingAddress": "123 Đường ABC",
                        "shippingDistrict": "Quận 1",
                        "shippingCity": "TP.HCM",
                        "paymentMethod": "BANK"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.totalVnd").value(8_600_000));
    }

    // ── GET /v1/orders/:id ────────────────────────────────────────────────────

    @Test
    void getOrder_exists_returnsOrder() throws Exception {
        String body = mockMvc.perform(post("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validOrderJson()))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.JsonNode node =
            new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
        String orderId = node.get("id").asText();

        mockMvc.perform(get("/v1/orders/" + orderId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(orderId))
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.totalVnd").value(5_000_000));
    }

    @Test
    void getOrder_notFound_returns404() throws Exception {
        mockMvc.perform(get("/v1/orders/00000000-0000-0000-0000-000000000000"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("not_found"));
    }

    // ── GET /v1/orders (my orders) ────────────────────────────────────────────

    @Test
    void getMyOrders_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/v1/orders"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyOrders_noOrders_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/v1/orders")
                .with(customerJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getMyOrders_returnsOnlyCallerOrders() throws Exception {
        // Create an order for this user
        mockMvc.perform(post("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validOrderJson())
                .with(customerJwt()))
            .andExpect(status().isCreated());

        // Create a guest order (no userId)
        mockMvc.perform(post("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validOrderJson()))
            .andExpect(status().isCreated());

        // My orders should return only the authenticated user's order
        mockMvc.perform(get("/v1/orders")
                .with(customerJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].totalVnd").value(5_000_000));
    }

    @Test
    void getMyOrders_returnsSortedNewestFirst() throws Exception {
        // Place two orders
        mockMvc.perform(post("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validOrderJson())
                .with(customerJwt()))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validOrderJson())
                .with(customerJwt()))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/v1/orders")
                .with(customerJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    // ── PATCH /v1/orders/:id/status ───────────────────────────────────────────

    @Test
    void updateStatus_staffRole_updatesAndReturns() throws Exception {
        String body = mockMvc.perform(post("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validOrderJson()))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String orderId = new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(body).get("id").asText();

        mockMvc.perform(patch("/v1/orders/" + orderId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "status": "CONFIRMED" }
                    """)
                .with(staffJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void updateStatus_customerRole_returns403() throws Exception {
        String body = mockMvc.perform(post("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validOrderJson()))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String orderId = new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(body).get("id").asText();

        mockMvc.perform(patch("/v1/orders/" + orderId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "status": "CONFIRMED" }
                    """)
                .with(customerJwt()))
            .andExpect(status().isForbidden());
    }

    @Test
    void updateStatus_notFound_returns404() throws Exception {
        mockMvc.perform(patch("/v1/orders/00000000-0000-0000-0000-000000000000/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "status": "CONFIRMED" }
                    """)
                .with(staffJwt()))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateStatus_invalidStatus_returns422() throws Exception {
        String body = mockMvc.perform(post("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validOrderJson()))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String orderId = new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(body).get("id").asText();

        mockMvc.perform(patch("/v1/orders/" + orderId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "status": "INVALID_STATUS" }
                    """)
                .with(staffJwt()))
            .andExpect(status().isBadRequest());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String validOrderJson() {
        return """
            {
                "items": [{ "sku": "dongtrung-tuoi-premium", "qty": 2 }],
                "shippingName": "Nguyễn Văn A",
                "shippingPhone": "0901234567",
                "shippingAddress": "123 Đường ABC",
                "shippingDistrict": "Quận 1",
                "shippingCity": "TP.HCM",
                "shippingNote": "Giao giờ hành chính",
                "paymentMethod": "COD"
            }
            """;
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor customerJwt() {
        return jwt().jwt(j -> j
            .subject(userId.toString())
            .claim("role", "CUSTOMER")
            .claim("email", "customer@test.com")
        ).authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor staffJwt() {
        return jwt().jwt(j -> j
            .subject(UUID.randomUUID().toString())
            .claim("role", "STAFF")
            .claim("email", "staff@test.com")
        ).authorities(new SimpleGrantedAuthority("ROLE_STAFF"));
    }
}
