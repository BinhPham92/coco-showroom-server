package com.cocoshowroom.server.product;

import com.cocoshowroom.server.config.AppProperties;
import com.cocoshowroom.server.shared.ConflictException;
import com.cocoshowroom.server.shared.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final ProductRepository productRepository;
    private final AppProperties appProperties;

    // ── Read ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ProductListResponse list(
        Category category, Grade grade, Boolean featured,
        String cursorStr, int limit
    ) {
        int clampedLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        Instant cursor = cursorStr != null ? decodeCursor(cursorStr) : null;

        // Fetch one extra row to determine whether a next page exists
        List<Product> rows = productRepository.findPage(
            category, grade, featured, cursor,
            PageRequest.of(0, clampedLimit + 1)
        );

        boolean hasNext = rows.size() > clampedLimit;
        List<Product> page = hasNext ? rows.subList(0, clampedLimit) : rows;

        String nextCursor = hasNext
            ? encodeCursor(page.getLast().getCreatedAt())
            : null;

        List<ProductResponse> items = page.stream()
            .map(p -> ProductResponse.from(p, storageBaseUrl()))
            .toList();

        return new ProductListResponse(items, nextCursor);
    }

    @Transactional(readOnly = true)
    public ProductResponse findBySlug(String slug) {
        Product p = productRepository.findBySlug(slug)
            .orElseThrow(() -> new NotFoundException("Product not found: " + slug));
        return ProductResponse.from(p, storageBaseUrl());
    }

    // ── Write ─────────────────────────────────────────────────────────────

    @Transactional
    public ProductResponse create(ProductRequest req) {
        if (productRepository.existsBySlug(req.slug())) {
            throw new ConflictException("Slug already in use: " + req.slug());
        }
        Product p = applyRequest(new Product(), req);
        productRepository.save(p);
        return ProductResponse.from(p, storageBaseUrl());
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest req) {
        Product p = productRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Product not found: " + id));

        if (!p.getSlug().equals(req.slug()) && productRepository.existsBySlug(req.slug())) {
            throw new ConflictException("Slug already in use: " + req.slug());
        }

        applyRequest(p, req);
        return ProductResponse.from(p, storageBaseUrl());
    }

    @Transactional
    public void delete(UUID id) {
        if (!productRepository.existsById(id)) {
            throw new NotFoundException("Product not found: " + id);
        }
        productRepository.deleteById(id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Product applyRequest(Product p, ProductRequest r) {
        p.setSlug(r.slug());
        p.setNameVi(r.nameVi());
        p.setNameEn(r.nameEn());
        p.setDescriptionVi(r.descriptionVi());
        p.setDescriptionEn(r.descriptionEn());
        p.setCategory(r.category());
        p.setGrade(r.grade());
        p.setPriceVnd(r.priceVnd());
        p.setSalePriceVnd(r.salePriceVnd());
        p.setWeightGrams(r.weightGrams());
        p.setInStock(r.inStock());
        p.setImages(r.images());
        p.setTags(r.tags());
        p.setFeatured(r.featured());
        return p;
    }

    private String storageBaseUrl() {
        return appProperties.getStorage().getBaseUrl();
    }

    /**
     * Cursor = URL-safe Base64 of the ISO-8601 createdAt timestamp.
     * Stable ordering is on createdAt DESC; ties are rare in a managed catalog.
     */
    private Instant decodeCursor(String cursor) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(cursor);
            return Instant.parse(new String(bytes, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid pagination cursor");
        }
    }

    private String encodeCursor(Instant createdAt) {
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(createdAt.toString().getBytes(StandardCharsets.UTF_8));
    }
}
