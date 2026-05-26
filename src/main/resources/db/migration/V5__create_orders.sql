-- W5: orders + order_items tables.
-- user_id is nullable — guest checkout is supported (no account required).
-- All product fields on order_items are snapshotted at order time so the record
-- stays accurate even if the product is later edited or removed.

CREATE TABLE orders (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID                      REFERENCES users(id) ON DELETE SET NULL,
    status            VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
                                    CHECK (status IN ('PENDING','CONFIRMED','PROCESSING','SHIPPED','DELIVERED','CANCELLED')),
    shipping_name     VARCHAR(255)  NOT NULL,
    shipping_phone    VARCHAR(20)   NOT NULL,
    shipping_address  TEXT          NOT NULL,
    shipping_district VARCHAR(255)  NOT NULL,
    shipping_city     VARCHAR(255)  NOT NULL,
    shipping_note     TEXT,
    payment_method    VARCHAR(10)   NOT NULL CHECK (payment_method IN ('COD','BANK')),
    total_vnd         BIGINT        NOT NULL CHECK (total_vnd >= 0),
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_orders_user_id    ON orders (user_id);
CREATE INDEX idx_orders_status     ON orders (status);
CREATE INDEX idx_orders_created_at ON orders (created_at DESC);

CREATE TABLE order_items (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID         NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id  UUID                      REFERENCES products(id) ON DELETE SET NULL,
    sku         VARCHAR(255) NOT NULL,   -- slug snapshot
    name_vi     VARCHAR(255) NOT NULL,   -- name snapshot
    name_en     VARCHAR(255) NOT NULL,
    qty         INTEGER      NOT NULL CHECK (qty > 0),
    price_vnd   BIGINT       NOT NULL CHECK (price_vnd > 0)  -- price snapshot at order time
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
