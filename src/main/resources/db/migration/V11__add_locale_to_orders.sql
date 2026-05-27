-- Locale for the order-confirmation email: 'vi' (default) or 'en'.
ALTER TABLE orders ADD COLUMN locale VARCHAR(5) NOT NULL DEFAULT 'vi';
