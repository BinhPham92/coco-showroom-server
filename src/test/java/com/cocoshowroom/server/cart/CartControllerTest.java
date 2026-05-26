package com.cocoshowroom.server.cart;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CartControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired CartItemRepository cartItemRepository;
    @Autowired UserRepository userRepository;
    @Autowired ProductRepository productRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private UUID userId;
    private Product product;

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("customer@test.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
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

    // ── GET /v1/cart ──────────────────────────────────────────────────────────

    @Test
    void getCart_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/v1/cart"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void getCart_emptyCart_returnsEmptyArray() throws Exception {
        mockMvc.perform(get("/v1/cart").with(customerJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    // ── POST /v1/cart/reconcile ───────────────────────────────────────────────

    @Test
    void reconcile_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/v1/cart/reconcile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "items": [{ "sku": "dongtrung-tuoi-premium", "qty": 2, "addedAt": 1716000000000 }] }
                    """))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void reconcile_emptyClientCart_returnsEmptyServerCart() throws Exception {
        mockMvc.perform(post("/v1/cart/reconcile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"items\": [] }")
                .with(customerJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void reconcile_clientItem_persistedAndReturned() throws Exception {
        mockMvc.perform(post("/v1/cart/reconcile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "items": [{ "sku": "dongtrung-tuoi-premium", "qty": 3, "addedAt": 1716000000000 }] }
                    """)
                .with(customerJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].sku").value("dongtrung-tuoi-premium"))
            .andExpect(jsonPath("$[0].qty").value(3))
            .andExpect(jsonPath("$[0].nameVi").value("Đông trùng tươi cao cấp"))
            .andExpect(jsonPath("$[0].nameEn").value("Fresh Cordyceps Premium"))
            .andExpect(jsonPath("$[0].priceVnd").value(2_500_000));

        assertThat(cartItemRepository.findAllByUserId(userId)).hasSize(1);
    }

    @Test
    void reconcile_unknownSku_silentlySkipped() throws Exception {
        mockMvc.perform(post("/v1/cart/reconcile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "items": [{ "sku": "does-not-exist", "qty": 1, "addedAt": 1716000000000 }] }
                    """)
                .with(customerJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void reconcile_clientWinsOnConflict_serverQtyOverwritten() throws Exception {
        // Pre-seed server cart with qty=1
        mockMvc.perform(post("/v1/cart/reconcile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "items": [{ "sku": "dongtrung-tuoi-premium", "qty": 1, "addedAt": 1716000000000 }] }
                    """)
                .with(customerJwt()))
            .andExpect(status().isOk());

        // Client reconciles again with qty=5 — should win
        mockMvc.perform(post("/v1/cart/reconcile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "items": [{ "sku": "dongtrung-tuoi-premium", "qty": 5, "addedAt": 1716000000001 }] }
                    """)
                .with(customerJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].qty").value(5));
    }

    @Test
    void reconcile_serverOnlyItem_preserved() throws Exception {
        // Seed a server-only item directly
        mockMvc.perform(put("/v1/cart/items/dongtrung-tuoi-premium")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"qty\": 2 }")
                .with(customerJwt()))
            .andExpect(status().isOk());

        // Reconcile with empty client cart — server item must survive
        mockMvc.perform(post("/v1/cart/reconcile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"items\": [] }")
                .with(customerJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].sku").value("dongtrung-tuoi-premium"))
            .andExpect(jsonPath("$[0].qty").value(2));
    }

    @Test
    void reconcile_invalidBody_returns422() throws Exception {
        mockMvc.perform(post("/v1/cart/reconcile")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "items": [{ "sku": "", "qty": 0, "addedAt": 0 }] }
                    """)
                .with(customerJwt()))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("validation_error"));
    }

    // ── PUT /v1/cart/items/{slug} ─────────────────────────────────────────────

    @Test
    void upsertItem_newItem_addsToCart() throws Exception {
        mockMvc.perform(put("/v1/cart/items/dongtrung-tuoi-premium")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"qty\": 2 }")
                .with(customerJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sku").value("dongtrung-tuoi-premium"))
            .andExpect(jsonPath("$.qty").value(2));
    }

    @Test
    void upsertItem_existingItem_updatesQty() throws Exception {
        // Add first
        mockMvc.perform(put("/v1/cart/items/dongtrung-tuoi-premium")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"qty\": 1 }")
                .with(customerJwt()))
            .andExpect(status().isOk());

        // Update qty
        mockMvc.perform(put("/v1/cart/items/dongtrung-tuoi-premium")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"qty\": 4 }")
                .with(customerJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.qty").value(4));

        assertThat(cartItemRepository.findAllByUserId(userId)).hasSize(1);
    }

    @Test
    void upsertItem_unknownProduct_returns404() throws Exception {
        mockMvc.perform(put("/v1/cart/items/no-such-product")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"qty\": 1 }")
                .with(customerJwt()))
            .andExpect(status().isNotFound());
    }

    @Test
    void upsertItem_zeroQty_returns422() throws Exception {
        mockMvc.perform(put("/v1/cart/items/dongtrung-tuoi-premium")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"qty\": 0 }")
                .with(customerJwt()))
            .andExpect(status().isUnprocessableEntity());
    }

    // ── DELETE /v1/cart/items/{slug} ──────────────────────────────────────────

    @Test
    void removeItem_existingItem_returns204AndRemoved() throws Exception {
        mockMvc.perform(put("/v1/cart/items/dongtrung-tuoi-premium")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"qty\": 1 }")
                .with(customerJwt()))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/v1/cart/items/dongtrung-tuoi-premium")
                .with(customerJwt()))
            .andExpect(status().isNoContent());

        assertThat(cartItemRepository.findAllByUserId(userId)).isEmpty();
    }

    @Test
    void removeItem_nonExistentItem_returns404() throws Exception {
        mockMvc.perform(delete("/v1/cart/items/does-not-exist")
                .with(customerJwt()))
            .andExpect(status().isNotFound());
    }

    // ── DELETE /v1/cart ───────────────────────────────────────────────────────

    @Test
    void clearCart_removesAllItems() throws Exception {
        mockMvc.perform(put("/v1/cart/items/dongtrung-tuoi-premium")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"qty\": 2 }")
                .with(customerJwt()))
            .andExpect(status().isOk());

        mockMvc.perform(delete("/v1/cart").with(customerJwt()))
            .andExpect(status().isNoContent());

        assertThat(cartItemRepository.findAllByUserId(userId)).isEmpty();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private org.springframework.test.web.servlet.request.RequestPostProcessor customerJwt() {
        return jwt().jwt(j -> j
            .subject(userId.toString())
            .claim("role", "CUSTOMER")
            .claim("email", "customer@test.com")
        );
    }
}
