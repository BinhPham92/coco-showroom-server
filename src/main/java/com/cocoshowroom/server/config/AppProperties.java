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

    @Getter
    @Setter
    public static class Cors {
        /** Comma-separated list of allowed origins, e.g. http://localhost:3031,https://cocoshowroom.vn */
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
         * Local dev: http://localhost:3031 (Next.js public folder)
         * Production: https://cdn.cocoshowroom.vn (S3/R2 CDN)
         */
        private String baseUrl = "http://localhost:3031";
    }

    @Getter
    @Setter
    public static class Admin {
        /** E-mail for the seeded STAFF account (created on first boot if missing). */
        private String email;
        /** Plain-text password — BCrypt-hashed by AdminUserInitializer at runtime. */
        private String password;
    }
}
