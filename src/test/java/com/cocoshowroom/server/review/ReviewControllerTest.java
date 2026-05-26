package com.cocoshowroom.server.review;

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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReviewControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ReviewRepository reviewRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;
    private UUID userId;
    private Product product;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
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
        product.setImages(List.of("products/photo.jpg"));
        product.setTags(List.of("tươi"));
        productRepository.save(product);
    }

    // ── GET /v1/products/:slug/reviews ────────────────────────────────────────

    @Test
    void getReviews_noReviews_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/v1/products/dongtrung-tuoi-premium/reviews"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getReviews_unknownSlug_returns404() throws Exception {
        mockMvc.perform(get("/v1/products/does-not-exist/reviews"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("not_found"));
    }

    @Test
    void getReviews_onlyApprovedReviewsAreReturned() throws Exception {
        // PENDING review — must not appear
        Review pending = new Review();
        pending.setProductId(product.getId());
        pending.setAuthorName("Pending Reviewer");
        pending.setRating(3);
        reviewRepository.save(pending);

        // APPROVED review — must appear
        Review approved = new Review();
        approved.setProductId(product.getId());
        approved.setAuthorName("Approved Reviewer");
        approved.setRating(5);
        approved.setStatus(ReviewStatus.APPROVED);
        reviewRepository.save(approved);

        // REJECTED review — must not appear
        Review rejected = new Review();
        rejected.setProductId(product.getId());
        rejected.setAuthorName("Rejected Reviewer");
        rejected.setRating(1);
        rejected.setStatus(ReviewStatus.REJECTED);
        reviewRepository.save(rejected);

        mockMvc.perform(get("/v1/products/dongtrung-tuoi-premium/reviews"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].authorName").value("Approved Reviewer"));
    }

    // ── POST /v1/products/:slug/reviews ───────────────────────────────────────

    @Test
    void createReview_guest_returns201AsPendingAndPersists() throws Exception {
        mockMvc.perform(post("/v1/products/dongtrung-tuoi-premium/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "authorName": "Khách hàng",
                        "rating": 5,
                        "body": "Chất lượng tuyệt vời!"
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.authorName").value("Khách hàng"))
            .andExpect(jsonPath("$.rating").value(5))
            .andExpect(jsonPath("$.body").value("Chất lượng tuyệt vời!"))
            .andExpect(jsonPath("$.status").value("PENDING"))  // not yet visible publicly
            .andExpect(jsonPath("$.createdAt").isNotEmpty());

        assertThat(reviewRepository.count()).isEqualTo(1);

        // Verify it does NOT appear in the public listing yet
        mockMvc.perform(get("/v1/products/dongtrung-tuoi-premium/reviews"))
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void createReview_authenticated_noProfileName_usesRequestAuthorName() throws Exception {
        // User has no profile name set in setUp — falls back to request body
        mockMvc.perform(post("/v1/products/dongtrung-tuoi-premium/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "authorName": "Logged In User", "rating": 4 }
                    """)
                .with(customerJwt()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.rating").value(4))
            .andExpect(jsonPath("$.authorName").value("Logged In User"));
    }

    @Test
    void createReview_authenticatedWithProfileName_ignoresRequestAuthorName() throws Exception {
        // Set a profile name on the user
        User user = userRepository.findById(userId).orElseThrow();
        user.setName("Nguyễn Văn B");
        userRepository.save(user);

        // Attempt to impersonate someone via the request body — server overrides it
        mockMvc.perform(post("/v1/products/dongtrung-tuoi-premium/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "authorName": "Admin", "rating": 5 }
                    """)
                .with(customerJwt()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.authorName").value("Nguyễn Văn B"));
    }

    @Test
    void createReview_guestCanReviewMultipleTimes() throws Exception {
        // Guests have no userId, so duplicate check is skipped
        mockMvc.perform(post("/v1/products/dongtrung-tuoi-premium/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "authorName": "Guest", "rating": 5 }
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/v1/products/dongtrung-tuoi-premium/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "authorName": "Guest Again", "rating": 3 }
                    """))
            .andExpect(status().isCreated());

        assertThat(reviewRepository.count()).isEqualTo(2);
    }

    @Test
    void createReview_authenticatedDuplicate_returns409() throws Exception {
        // First review
        mockMvc.perform(post("/v1/products/dongtrung-tuoi-premium/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "authorName": "User", "rating": 5 }
                    """)
                .with(customerJwt()))
            .andExpect(status().isCreated());

        // Second review from same user — must be rejected
        mockMvc.perform(post("/v1/products/dongtrung-tuoi-premium/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "authorName": "User", "rating": 4 }
                    """)
                .with(customerJwt()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("conflict"));
    }

    @Test
    void createReview_unknownSlug_returns404() throws Exception {
        mockMvc.perform(post("/v1/products/does-not-exist/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "authorName": "Test", "rating": 3 }
                    """))
            .andExpect(status().isNotFound());
    }

    @Test
    void createReview_ratingOutOfRange_returns422() throws Exception {
        mockMvc.perform(post("/v1/products/dongtrung-tuoi-premium/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "authorName": "Test", "rating": 6 }
                    """))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("validation_error"));
    }

    @Test
    void createReview_blankAuthorName_returns422() throws Exception {
        mockMvc.perform(post("/v1/products/dongtrung-tuoi-premium/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "authorName": "", "rating": 4 }
                    """))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.code").value("validation_error"));
    }

    // ── PATCH /v1/products/:slug/reviews/:id/status ───────────────────────────

    @Test
    void moderateReview_staffApproves_reviewBecomesPublic() throws Exception {
        // Create a pending review via the API
        String body = mockMvc.perform(post("/v1/products/dongtrung-tuoi-premium/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "authorName": "Test User", "rating": 5 }
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String reviewId = new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(body).get("id").asText();

        // Not yet visible
        mockMvc.perform(get("/v1/products/dongtrung-tuoi-premium/reviews"))
            .andExpect(jsonPath("$.length()").value(0));

        // Staff approves
        mockMvc.perform(patch("/v1/products/dongtrung-tuoi-premium/reviews/" + reviewId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "status": "APPROVED" }
                    """)
                .with(staffJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));

        // Now visible
        mockMvc.perform(get("/v1/products/dongtrung-tuoi-premium/reviews"))
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].authorName").value("Test User"));
    }

    @Test
    void moderateReview_staffRejects_reviewRemainsHidden() throws Exception {
        String body = mockMvc.perform(post("/v1/products/dongtrung-tuoi-premium/reviews")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "authorName": "Spammer", "rating": 1, "body": "SPAM" }
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();

        String reviewId = new com.fasterxml.jackson.databind.ObjectMapper()
            .readTree(body).get("id").asText();

        mockMvc.perform(patch("/v1/products/dongtrung-tuoi-premium/reviews/" + reviewId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "status": "REJECTED" }
                    """)
                .with(staffJwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(get("/v1/products/dongtrung-tuoi-premium/reviews"))
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void moderateReview_customerRole_returns403() throws Exception {
        Review review = new Review();
        review.setProductId(product.getId());
        review.setAuthorName("Test");
        review.setRating(4);
        UUID reviewId = reviewRepository.save(review).getId();

        mockMvc.perform(patch("/v1/products/dongtrung-tuoi-premium/reviews/" + reviewId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "status": "APPROVED" }
                    """)
                .with(customerJwt()))
            .andExpect(status().isForbidden());
    }

    @Test
    void moderateReview_notFound_returns404() throws Exception {
        mockMvc.perform(patch("/v1/products/dongtrung-tuoi-premium/reviews/00000000-0000-0000-0000-000000000000/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "status": "APPROVED" }
                    """)
                .with(staffJwt()))
            .andExpect(status().isNotFound());
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private RequestPostProcessor customerJwt() {
        return jwt().jwt(j -> j
            .subject(userId.toString())
            .claim("role", "CUSTOMER")
        ).authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    }

    private RequestPostProcessor staffJwt() {
        return jwt().jwt(j -> j
            .subject(UUID.randomUUID().toString())
            .claim("role", "STAFF")
        ).authorities(new SimpleGrantedAuthority("ROLE_STAFF"));
    }
}
