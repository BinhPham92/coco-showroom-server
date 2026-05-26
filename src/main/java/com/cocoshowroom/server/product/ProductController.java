package com.cocoshowroom.server.product;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ── Public ────────────────────────────────────────────────────────────

    @GetMapping
    public ProductListResponse list(
        @RequestParam(required = false) Category category,
        @RequestParam(required = false) Grade grade,
        @RequestParam(required = false) Boolean featured,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(required = false) String cursor
    ) {
        return productService.list(category, grade, featured, cursor, limit);
    }

    @GetMapping("/{slug}")
    public ProductResponse findBySlug(@PathVariable String slug) {
        return productService.findBySlug(slug);
    }

    // ── Staff only (requires JWT with role=STAFF) ─────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STAFF')")
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('STAFF')")
    public ProductResponse update(
        @PathVariable UUID id,
        @Valid @RequestBody ProductRequest request
    ) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('STAFF')")
    public void delete(@PathVariable UUID id) {
        productService.delete(id);
    }
}
