-- W4: cart_items — persists each authenticated user's cart server-side.
-- One row per (user, product) pair; qty is the current quantity.
-- ON DELETE CASCADE keeps referential integrity when a user or product is removed.
CREATE TABLE cart_items (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id)    ON DELETE CASCADE,
    product_id  UUID        NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    qty         INTEGER     NOT NULL CHECK (qty > 0),
    added_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_cart_user_product UNIQUE (user_id, product_id)
);

CREATE INDEX idx_cart_items_user_id ON cart_items (user_id);
