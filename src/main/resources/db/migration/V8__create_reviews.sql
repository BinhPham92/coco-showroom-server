CREATE TABLE reviews (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id   UUID         NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    user_id      UUID         REFERENCES users(id) ON DELETE SET NULL,
    author_name  VARCHAR(200) NOT NULL,
    rating       INTEGER      NOT NULL CHECK (rating >= 1 AND rating <= 5),
    body         TEXT,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                     CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX reviews_product_id_idx ON reviews(product_id);
