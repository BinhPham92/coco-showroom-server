package com.cocoshowroom.server.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RateLimitingFilter}.
 *
 * <p>Uses the package-visible constructor to inject a capacity-1 bucket factory,
 * which makes it trivial to exhaust the limit with just two requests without
 * looping 20+ times.
 */
class RateLimitingFilterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Factory that creates a bucket allowing exactly 1 request per 10 minutes. */
    private static final Supplier<Bucket> LIMIT_OF_1 = () -> Bucket.builder()
        .addLimit(Bandwidth.classic(1, Refill.intervally(1, Duration.ofMinutes(10))))
        .build();

    /** Factory that creates a bucket allowing exactly 2 requests per 10 minutes. */
    private static final Supplier<Bucket> LIMIT_OF_2 = () -> Bucket.builder()
        .addLimit(Bandwidth.classic(2, Refill.intervally(2, Duration.ofMinutes(10))))
        .build();

    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter(MAPPER, LIMIT_OF_1, LIMIT_OF_1);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Sends a single POST through the filter and returns the response.
     * A fresh {@link MockHttpServletRequest} is used every call so
     * {@link org.springframework.web.filter.OncePerRequestFilter}'s
     * already-filtered guard never fires.
     */
    private MockHttpServletResponse doPost(String uri, String remoteAddr) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", uri);
        req.setRemoteAddr(remoteAddr);
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, (r, resp) -> { /* chain no-op */ });
        return res;
    }

    // ── GET requests are never rate-limited ──────────────────────────────────

    @Test
    void get_to_auth_social_is_not_rate_limited() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/v1/auth/social");
        req.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse res = new MockHttpServletResponse();
        boolean[] chainCalled = {false};
        filter.doFilter(req, res, (r, resp) -> chainCalled[0] = true);

        assertThat(chainCalled[0]).isTrue();
        assertThat(res.getStatus()).isNotEqualTo(429);
    }

    // ── Unlisted paths pass through ──────────────────────────────────────────

    @Test
    void post_to_unlisted_path_passes_through() throws Exception {
        MockHttpServletResponse res = doPost("/v1/products", "1.2.3.4");
        assertThat(res.getStatus()).isNotEqualTo(429);
    }

    @Test
    void post_to_get_reviews_path_passes_through() throws Exception {
        // GET /v1/products/{slug}/reviews is public — only POST is rate-limited
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/v1/products/some-slug/reviews");
        req.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse res = new MockHttpServletResponse();
        boolean[] chainCalled = {false};
        filter.doFilter(req, res, (r, resp) -> chainCalled[0] = true);

        assertThat(chainCalled[0]).isTrue();
        assertThat(res.getStatus()).isNotEqualTo(429);
    }

    // ── auth/social rate limiting ─────────────────────────────────────────────

    @Test
    void auth_social_allows_first_request() throws Exception {
        boolean[] chainCalled = {false};
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/v1/auth/social");
        req.setRemoteAddr("1.2.3.4");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, (r, resp) -> chainCalled[0] = true);

        assertThat(chainCalled[0]).isTrue();
        assertThat(res.getStatus()).isNotEqualTo(429);
    }

    @Test
    void auth_social_blocks_when_limit_exceeded() throws Exception {
        doPost("/v1/auth/social", "1.2.3.4");           // consume the 1 token
        MockHttpServletResponse res = doPost("/v1/auth/social", "1.2.3.4");  // should be blocked

        assertThat(res.getStatus()).isEqualTo(429);
    }

    @Test
    void auth_social_429_includes_retry_after_header() throws Exception {
        doPost("/v1/auth/social", "1.2.3.4");
        MockHttpServletResponse res = doPost("/v1/auth/social", "1.2.3.4");

        String retryAfter = res.getHeader("Retry-After");
        assertThat(retryAfter).isNotNull();
        assertThat(Long.parseLong(retryAfter)).isGreaterThanOrEqualTo(1L);
    }

    @Test
    void auth_social_429_body_is_json_with_correct_error_code() throws Exception {
        doPost("/v1/auth/social", "1.2.3.4");
        MockHttpServletResponse res = doPost("/v1/auth/social", "1.2.3.4");

        assertThat(res.getContentType()).contains("application/json");
        assertThat(res.getContentAsString()).contains("rate_limit_exceeded");
    }

    @Test
    void auth_social_allows_requests_within_limit_of_two() throws Exception {
        filter = new RateLimitingFilter(MAPPER, LIMIT_OF_2, LIMIT_OF_2);

        assertThat(doPost("/v1/auth/social", "1.2.3.4").getStatus()).isNotEqualTo(429);
        assertThat(doPost("/v1/auth/social", "1.2.3.4").getStatus()).isNotEqualTo(429);
        // 3rd request must be blocked
        assertThat(doPost("/v1/auth/social", "1.2.3.4").getStatus()).isEqualTo(429);
    }

    // ── reviews rate limiting ─────────────────────────────────────────────────

    @Test
    void reviews_blocks_when_limit_exceeded() throws Exception {
        doPost("/v1/products/some-slug/reviews", "1.2.3.4");
        MockHttpServletResponse res = doPost("/v1/products/some-slug/reviews", "1.2.3.4");

        assertThat(res.getStatus()).isEqualTo(429);
    }

    @Test
    void reviews_rate_limit_matches_any_product_slug() throws Exception {
        doPost("/v1/products/cordyceps-militaris-premium/reviews", "2.3.4.5");
        MockHttpServletResponse res = doPost("/v1/products/cordyceps-militaris-premium/reviews", "2.3.4.5");

        assertThat(res.getStatus()).isEqualTo(429);
    }

    // ── IP isolation ──────────────────────────────────────────────────────────

    @Test
    void different_ips_have_independent_buckets() throws Exception {
        doPost("/v1/auth/social", "1.2.3.4");  // exhaust IP 1

        // IP 2 should still be allowed
        boolean[] chainCalled = {false};
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/v1/auth/social");
        req.setRemoteAddr("5.6.7.8");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, (r, resp) -> chainCalled[0] = true);

        assertThat(chainCalled[0]).isTrue();
        assertThat(res.getStatus()).isNotEqualTo(429);
    }

    // ── X-Forwarded-For ───────────────────────────────────────────────────────

    @Test
    void x_forwarded_for_takes_precedence_over_remote_addr() throws Exception {
        // First request from real client IP 203.0.113.1 (via proxy 10.0.0.1)
        MockHttpServletRequest req1 = new MockHttpServletRequest("POST", "/v1/auth/social");
        req1.setRemoteAddr("10.0.0.1");
        req1.addHeader("X-Forwarded-For", "203.0.113.1");
        filter.doFilter(req1, new MockHttpServletResponse(), (r, resp) -> { });

        // Second request from same real IP — should be blocked
        MockHttpServletRequest req2 = new MockHttpServletRequest("POST", "/v1/auth/social");
        req2.setRemoteAddr("10.0.0.1");   // same proxy
        req2.addHeader("X-Forwarded-For", "203.0.113.1");  // same real client
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        filter.doFilter(req2, res2, (r, resp) -> { });

        assertThat(res2.getStatus()).isEqualTo(429);
    }

    @Test
    void xff_leftmost_entry_is_used_when_list_is_present() {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/v1/auth/social");
        req.setRemoteAddr("10.0.0.1");
        req.addHeader("X-Forwarded-For", "203.0.113.42, 172.16.0.5, 192.168.1.1");

        assertThat(RateLimitingFilter.clientIp(req)).isEqualTo("203.0.113.42");
    }

    @Test
    void remote_addr_used_when_xff_absent() {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/v1/auth/social");
        req.setRemoteAddr("99.88.77.66");

        assertThat(RateLimitingFilter.clientIp(req)).isEqualTo("99.88.77.66");
    }
}
