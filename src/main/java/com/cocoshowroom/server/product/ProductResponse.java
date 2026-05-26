package com.cocoshowroom.server.product;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Public-facing product representation.
 * Storage keys in images are resolved to full URLs at serialization time.
 */
public record ProductResponse(
    UUID id,
    String slug,
    String nameVi,
    String nameEn,
    String descriptionVi,
    String descriptionEn,
    String category,
    String grade,
    long priceVnd,
    Long salePriceVnd,
    int weightGrams,
    boolean inStock,
    List<String> images,   // full URLs — storageBaseUrl + "/" + key
    List<String> tags,
    boolean featured,
    Instant createdAt
) {
    static ProductResponse from(Product p, String storageBaseUrl) {
        List<String> resolvedImages = p.getImages().stream()
            .map(key -> storageBaseUrl + "/" + key)
            .toList();

        return new ProductResponse(
            p.getId(),
            p.getSlug(),
            p.getNameVi(),
            p.getNameEn(),
            p.getDescriptionVi(),
            p.getDescriptionEn(),
            p.getCategory().name(),
            p.getGrade().name(),
            p.getPriceVnd(),
            p.getSalePriceVnd(),
            p.getWeightGrams(),
            p.isInStock(),
            resolvedImages,
            p.getTags(),
            p.isFeatured(),
            p.getCreatedAt()
        );
    }
}
