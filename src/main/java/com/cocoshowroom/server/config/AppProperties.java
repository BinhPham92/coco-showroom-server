package com.cocoshowroom.server.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed wrapper for all app.* properties.
 * Injected via constructor in SecurityConfig and other beans.
 */
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private Cors cors = new Cors();
    private Jwt jwt = new Jwt();
    private Storage storage = new Storage();
    private Admin admin = new Admin();
    private Mail mail = new Mail();

    @Getter
    @Setter
    public static class Cors {
        /** Comma-separated list of allowed origins, e.g. {@code http://localhost:3031,https://cocoshowroom.vn} */
        private String origins = "http://localhost:3031";
    }

    @Getter
    @Setter
    public static class Jwt {
        /** HS256 secret — must be ≥ 32 chars in production. */
        private String secret;
        private int expirationHours = 24;
    }

    @Getter
    @Setter
    public static class Storage {
        /**
         * Base URL prepended to image storage keys — no trailing slash.
         * Local dev: {@code http://localhost:3031} (Next.js public folder)
         * Production: {@code https://cdn.cocoshowroom.vn} (S3/R2 CDN)
         */
        private String baseUrl = "http://localhost:3031";
    }

    @Getter
    @Setter
    public static class Mail {
        /** Envelope from-address for all outbound transactional emails. */
        private String fromAddress = "no-reply@cocoshowroom.vn";
        /** Display name shown in the From header. */
        private String fromName = "Coco Showroom";
    }

    private OAuth oauth = new OAuth();

    @Getter
    @Setter
    public static class Admin {
        /** Email for the auto-seeded STAFF identity. */
        private String email;
        /** OAuth provider the admin will use to sign in (e.g. "google", "facebook"). */
        private String provider = "google";
    }

    @Getter
    @Setter
    public static class OAuth {
        private Google google = new Google();
        private Facebook facebook = new Facebook();

        @Getter
        @Setter
        public static class Google {
            /** Google OAuth client ID — used to verify the 'aud' claim of id_tokens. */
            private String clientId;
        }

        @Getter
        @Setter
        public static class Facebook {
            /** Facebook App ID — validated against the token's debug_token response. */
            private String appId;
            /**
             * Facebook App Secret — combined with appId as the App Access Token
             * ({@code appId|appSecret}) for server-to-server token introspection.
             * Must be kept secret and never sent to the client.
             */
            private String appSecret;
        }
    }
}
