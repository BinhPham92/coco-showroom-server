package com.cocoshowroom.server.admin;

import com.cocoshowroom.server.auth.Role;
import com.cocoshowroom.server.auth.User;
import com.cocoshowroom.server.auth.UserRepository;
import com.cocoshowroom.server.product.Category;
import com.cocoshowroom.server.product.Grade;
import com.cocoshowroom.server.product.Product;
import com.cocoshowroom.server.product.ProductRepository;
import com.cocoshowroom.server.review.Review;
import com.cocoshowroom.server.review.ReviewRepository;
import com.cocoshowroom.server.review.ReviewStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminReviewControllerTest {

    @Autowired MockMvc          mockMvc;
    @Autowired ReviewRepository reviewRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository   userRepository;

    private UUID productId;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        Product product = new Product();
        product.setSlug("dongtrung-tuoi-premium");
        product.setNameVi("Đông trùng tươi");
        product.setNameEn("Fresh Cordyceps");
        product.setDescriptionVi("Mô tả");
        product.setDescriptionEn("Description");
        product.setCategory(Category.fresh);
        product.setGrade(Grade.premium);
        product.setPriceVnd(2_500_000L);
        product.setWeightGrams(50);
        product.setInStock(true);
        product.setImages(List.of("products/photo.jpg"));
        product.setTags(List.of("tươi"));
        productId = productRepository.save(product).getId();
    }

    // ── Auth ─────────────────────────────────────────────────────────────────

    @Test
    void list_noJwt_returns401() throws Exception {
        mockMvc.perform(get("/v1/admin/reviews"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_customerRole_returns403() throws Exception {
        mockMvc.perform(get("/v1/admin/reviews")
                        .with(customerJwt()))
                .andExpect(status().isForbidden());
    }

    // ── Empty state ───────────────────────────────────────────────────────────

    @Test
    void list_noReviews_returnsEmptyPage() throws Exception {
        mockMvc.perform(get("/v1/admin/reviews")
                        .with(staffJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.nextCursor").doesNotExist())
                .andExpect(jsonPath("$.total").value(0));
    }

    // ── Full listing ──────────────────────────────────────────────────────────

    @Test
    void list_withReviews_returnsAll() throws Exception {
        saveReview(ReviewStatus.PENDING);
        saveReview(ReviewStatus.APPROVED);

        mockMvc.perform(get("/v1/admin/reviews")
                        .with(staffJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.total").value(2));
    }

    // ── Status filter ─────────────────────────────────────────────────────────

    @Test
    void list_pendingFilter_returnsOnlyPending() throws Exception {
        saveReview(ReviewStatus.PENDING);
        saveReview(ReviewStatus.PENDING);
        saveReview(ReviewStatus.APPROVED);

        mockMvc.perform(get("/v1/admin/reviews?status=PENDING")
                        .with(staffJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[0].status").value("PENDING"))
                .andExpect(jsonPath("$.items[1].status").value("PENDING"));
    }

    @Test
    void list_approvedFilter_returnsOnlyApproved() throws Exception {
        saveReview(ReviewStatus.PENDING);
        saveReview(ReviewStatus.APPROVED);

        mockMvc.perform(get("/v1/admin/reviews?status=APPROVED")
                        .with(staffJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].status").value("APPROVED"));
    }

    // ── Pagination ────────────────────────────────────────────────────────────

    @Test
    void list_limitOne_returnsNextCursor() throws Exception {
        saveReview(ReviewStatus.PENDING);
        saveReview(ReviewStatus.PENDING);

        String body = mockMvc.perform(get("/v1/admin/reviews?limit=1")
                        .with(staffJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.nextCursor").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String cursor = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(body).get("nextCursor").asText();

        mockMvc.perform(get("/v1/admin/reviews?limit=1&cursor=" + cursor)
                        .with(staffJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void saveReview(ReviewStatus status) {
        Review review = new Review();
        review.setProductId(productId);
        review.setAuthorName("Test User");
        review.setRating(5);
        review.setBody("Great product!");
        review.setStatus(status);
        reviewRepository.save(review);
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
