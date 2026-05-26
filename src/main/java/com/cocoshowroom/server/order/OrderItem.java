package com.cocoshowroom.server.order;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** Nullable — product may be deleted after the order was placed. */
    @Column(name = "product_id")
    private UUID productId;

    /** Product slug — snapshotted at order time. */
    @Column(nullable = false)
    private String sku;

    /** Product names snapshotted so the order record is self-contained. */
    @Column(nullable = false)
    private String nameVi;

    @Column(nullable = false)
    private String nameEn;

    @Column(nullable = false)
    private int qty;

    /** Price snapshotted at order time — never reflects later catalogue changes. */
    @Column(nullable = false)
    private long priceVnd;
}
