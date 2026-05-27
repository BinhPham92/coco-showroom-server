-- Contact email for order confirmation.
-- Existing rows (if any) get an empty string as a safe default before the default is dropped.
ALTER TABLE orders ADD COLUMN contact_email VARCHAR(320) NOT NULL DEFAULT '';
ALTER TABLE orders ALTER COLUMN contact_email DROP DEFAULT;
