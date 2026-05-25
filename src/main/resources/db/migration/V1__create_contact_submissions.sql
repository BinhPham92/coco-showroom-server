-- W1: contact submissions table
CREATE TABLE contact_submissions (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(255) NOT NULL,
    email      VARCHAR(320) NOT NULL,
    subject    VARCHAR(500) NOT NULL,
    message    TEXT         NOT NULL,
    ip         VARCHAR(45),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_contact_submissions_email      ON contact_submissions (email);
CREATE INDEX idx_contact_submissions_created_at ON contact_submissions (created_at DESC);
