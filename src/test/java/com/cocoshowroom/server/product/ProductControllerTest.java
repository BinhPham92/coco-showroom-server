package com.cocoshowroom.server.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    // ── Read ──────────────────────────────────────────────────────────────

    @Test
    void list_empty_returnsEmptyPage() throws Exception {
        mockMvc.perform(get("/v1/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.items.length()").value(0))
            .andExpect(jsonPath("$.nextCursor").doesNotExist());
    }

    @Test
    void list_returnsProductsWithResolvedImageUrls() throws Exception {
        createProduct("slug-1", Category.fresh, Grade.premium, true);

        mockMvc.perform(get("/v1/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].slug").value("slug-1"))
            .andExpect(jsonPath("$.items[0].images[0]").value("http://localhost:3031/products/photo.jpg"));
    }

    @Test
    void list_filterByCategory_returnsMatchingOnly() throws Exception {
        createProduct("fresh-1", Category.fresh, Grade.premium, false);
        createProduct("dried-1", Category.dried, Grade.standard, false);

        mockMvc.perform(get("/v1/products?category=fresh"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].slug").value("fresh-1"));
    }

    @Test
    void list_filterByFeatured_returnsOnlyFeatured() throws Exception {
        createProduct("featured-1", Category.fresh, Grade.premium, true);
        createProduct("normal-1",   Category.dried, Grade.standard, false);

        mockMvc.perform(get("/v1/products?featured=true"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].slug").value("featured-1"));
    }

    @Test
    void list_invalidCategory_returns400() throws Exception {
        mockMvc.perform(get("/v1/products?category=invalid"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void findBySlug_exists_returnsProduct() throws Exception {
        createProduct("my-slug", Category.dried, Grade.premium, false);

        mockMvc.perform(get("/v1/products/my-slug"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slug").value("my-slug"))
            .andExpect(jsonPath("$.category").value("dried"));
    }

    @Test
    void findBySlug_notFound_returns404() throws Exception {
        mockMvc.perform(get("/v1/products/does-not-exist"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("not_found"));
    }

    // ── Write — unauthenticated ────────────────────────────────────────────

    @Test
    void create_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validProductJson("no-auth-slug")))
            .andExpect(status().isUnauthorized());
    }

    // ── Write — authenticated as STAFF ────────────────────────────────────

    @Test
    @WithMockUser(roles = "STAFF")
    void create_validRequest_returns201AndPersists() throws Exception {
        mockMvc.perform(post("/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validProductJson("new-product")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.slug").value("new-product"))
            .andExpect(jsonPath("$.id").isNotEmpty());

        assertThat(productRepository.existsBySlug("new-product")).isTrue();
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void create_duplicateSlug_returns409() throws Exception {
        createProduct("existing-slug", Category.fresh, Grade.premium, false);

        mockMvc.perform(post("/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validProductJson("existing-slug")))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("conflict"));
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void delete_exists_returns204() throws Exception {
        Product p = createProduct("to-delete", Category.fresh, Grade.premium, false);

        mockMvc.perform(delete("/v1/products/" + p.getId()))
            .andExpect(status().isNoContent());

        assertThat(productRepository.existsById(p.getId())).isFalse();
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void delete_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/v1/products/00000000-0000-0000-0000-000000000000"))
            .andExpect(status().isNotFound());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Product createProduct(String slug, Category category, Grade grade, boolean featured) {
        Product p = new Product();
        p.setSlug(slug);
        p.setNameVi("Sản phẩm " + slug);
        p.setNameEn("Product " + slug);
        p.setDescriptionVi("Mô tả");
        p.setDescriptionEn("Description");
        p.setCategory(category);
        p.setGrade(grade);
        p.setPriceVnd(1_000_000L);
        p.setWeightGrams(100);
        p.setInStock(true);
        p.setImages(java.util.List.of("products/photo.jpg"));
        p.setTags(java.util.List.of("test"));
        p.setFeatured(featured);
        return productRepository.save(p);
    }

    private String validProductJson(String slug) {
        return """
            {
                "slug": "%s",
                "nameVi": "Tên sản phẩm",
                "nameEn": "Product name",
                "descriptionVi": "Mô tả sản phẩm",
                "descriptionEn": "Product description",
                "category": "fresh",
                "grade": "premium",
                "priceVnd": 1500000,
                "weightGrams": 50,
                "inStock": true,
                "images": ["products/photo.jpg"],
                "tags": ["tươi", "premium"],
                "featured": false
            }
            """.formatted(slug);
    }
}
