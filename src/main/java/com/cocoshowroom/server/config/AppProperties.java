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
}
