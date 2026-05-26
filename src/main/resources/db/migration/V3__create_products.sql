-- W2: products table
CREATE TABLE products (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    slug            VARCHAR(255) NOT NULL UNIQUE,
    name_vi         VARCHAR(255) NOT NULL,
    name_en         VARCHAR(255) NOT NULL,
    description_vi  TEXT         NOT NULL,
    description_en  TEXT         NOT NULL,
    category        VARCHAR(50)  NOT NULL CHECK (category IN ('fresh', 'dried', 'extract')),
    grade           VARCHAR(50)  NOT NULL CHECK (grade IN ('premium', 'standard')),
    price_vnd       BIGINT       NOT NULL CHECK (price_vnd > 0),
    sale_price_vnd  BIGINT                CHECK (sale_price_vnd > 0),
    weight_grams    INTEGER      NOT NULL CHECK (weight_grams > 0),
    in_stock        BOOLEAN      NOT NULL DEFAULT true,
    images          TEXT         NOT NULL DEFAULT '[]',  -- JSON array of storage keys; base URL prepended at serve time
    tags            TEXT         NOT NULL DEFAULT '[]',  -- JSON array
    featured        BOOLEAN      NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_products_slug       ON products (slug);
CREATE        INDEX idx_products_category   ON products (category);
CREATE        INDEX idx_products_grade      ON products (grade);
CREATE        INDEX idx_products_featured   ON products (featured) WHERE featured = true;
CREATE        INDEX idx_products_created_at ON products (created_at DESC);
