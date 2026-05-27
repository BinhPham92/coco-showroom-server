-- W13: idempotency key on orders to prevent duplicate order creation on client retry.
-- Nullable because existing and guest orders have no client-supplied key.
-- UNIQUE constraint enforces exactly-once semantics per key.

ALTER TABLE orders
    ADD COLUMN idempotency_key VARCHAR(64) NULL;

CREATE UNIQUE INDEX uidx_orders_idempotency_key
    ON orders (idempotency_key)
    WHERE idempotency_key IS NOT NULL;
