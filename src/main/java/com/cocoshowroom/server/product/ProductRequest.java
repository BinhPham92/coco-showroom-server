package com.cocoshowroom.server.product;

import jakarta.validation.constraints.*;

import java.util.List;

public record ProductRequest(
    @NotBlank @Size(max = 255) String slug,
    @NotBlank @Size(max = 255) String nameVi,
    @NotBlank @Size(max = 255) String nameEn,
    @NotBlank String descriptionVi,
    @NotBlank String descriptionEn,
    @NotNull Category category,
    @NotNull Grade grade,
    @NotNull @Positive Long priceVnd,
    Long salePriceVnd,               // optional sale price
    @NotNull @Positive Integer weightGrams,
    boolean inStock,
    @NotNull @Size(min = 1, max = 20) List<@NotBlank String> images,
    @NotNull List<@NotBlank String> tags,
    boolean featured
) {}
