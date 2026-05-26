package com.cocoshowroom.server.product;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.cocoshowroom.server.shared.StringListConverter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String nameVi;

    @Column(nullable = false)
    private String nameEn;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descriptionVi;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descriptionEn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Grade grade;

    @Column(nullable = false)
    private Long priceVnd;

    @Column
    private Long salePriceVnd;

    @Column(nullable = false)
    private Integer weightGrams;

    @Column(nullable = false)
    private boolean inStock = true;

    /** Storage keys (e.g. "products/photo.jpg"). Base URL prepended at response time. */
    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private List<String> images = new ArrayList<>();

    @Convert(converter = StringListConverter.class)
    @Column(nullable = false, columnDefinition = "TEXT")
    private List<String> tags = new ArrayList<>();

    @Column(nullable = false)
    private boolean featured = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
