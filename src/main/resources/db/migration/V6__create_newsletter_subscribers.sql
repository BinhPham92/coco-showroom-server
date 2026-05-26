CREATE TABLE newsletter_subscribers (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email          VARCHAR(320) NOT NULL,
    subscribed_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT newsletter_subscribers_email_key UNIQUE (email)
);
