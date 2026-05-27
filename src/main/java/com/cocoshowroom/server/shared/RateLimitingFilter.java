package com.cocoshowroom.server.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Servlet filter that enforces per-IP token-bucket rate limits on the two
 * endpoints most vulnerable to abuse.
 *
 * <ul>
 *   <li>{@code POST /v1/auth/social} — 20 requests / 10 min (credential-stuffing guard)</li>
 *   <li>{@code POST /v1/reviews}     — 5 requests / hour (guest review-spam guard)</li>
 * </ul>
 *
 * <p><b>IP extraction:</b> reads {@code X-Forwarded-For} first (leftmost entry),
 * then falls back to {@code RemoteAddr}. The Next.js BFF <em>must</em> forward
 * the original client IP in production; without it, all requests appear to come
 * from the BFF host and rate-limiting degrades to per-BFF rather than per-user.
 *
 * <p><b>Storage:</b> in-memory {@link ConcurrentHashMap} — zero dependencies,
 * but resets on restart and does not share state across multiple JVM instances.
 * At N replicas each instance allows the full quota, so effective limits are N×
 * the configured values. This is an accepted trade-off for single-node deployments.
 *
 * <p><b>Upgrade path to distributed rate-limiting:</b>
 * <ol>
 *   <li>Add {@code bucket4j-redis} (io.github.bucket4j:bucket4j-redis) and
 *       {@code spring-boot-starter-data-redis} dependencies.</li>
 *   <li>Configure a {@code RedissonClient} or {@code LettuceConnectionFactory} bean.</li>
 *   <li>Replace the two {@link ConcurrentHashMap}s with a
 *       {@code io.github.bucket4j.redis.redisson.cas.RedissonBasedProxyManager}
 *       (or its Lettuce variant).</li>
 *   <li>Replace {@code computeIfAbsent(ip, k -> factory.get())} calls with
 *       {@code proxyManager.builder().build(ip, configSupplier).tryConsumeAndReturnRemaining(1)}.</li>
 * </ol>
 *
 * <p>Runs just after {@link RequestIdFilter} ({@link Ordered#HIGHEST_PRECEDENCE} + 5)
 * so the {@code requestId} MDC key is already populated when a 429 is written.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String  AUTH_SOCIAL_PATH  = "/v1/auth/social";
    /** Matches {@code /v1/products/<slug>/reviews} — the review submission endpoint. */
    private static final String  REVIEWS_URI_REGEX = "/v1/products/[^/]+/reviews";

    // IP address → Bucket, one map per rate-limited endpoint
    final Map<String, Bucket> authBuckets   = new ConcurrentHashMap<>();
    final Map<String, Bucket> reviewBuckets = new ConcurrentHashMap<>();

    private final Supplier<Bucket> authBucketFactory;
    private final Supplier<Bucket> reviewBucketFactory;
    private final ObjectMapper     objectMapper;

    /** Production constructor — Spring picks this up via {@code @Component}. */
    @Autowired
    public RateLimitingFilter(ObjectMapper objectMapper) {
        this.objectMapper        = objectMapper;
        this.authBucketFactory   = RateLimitingFilter::newAuthBucket;
        this.reviewBucketFactory = RateLimitingFilter::newReviewBucket;
    }

    /** Package-visible constructor for unit tests with configurable limits. */
    RateLimitingFilter(ObjectMapper objectMapper,
                       Supplier<Bucket> authBucketFactory,
                       Supplier<Bucket> reviewBucketFactory) {
        this.objectMapper        = objectMapper;
        this.authBucketFactory   = authBucketFactory;
        this.reviewBucketFactory = reviewBucketFactory;
    }

    /**
     * Clears all per-IP buckets, effectively resetting the rate-limit state.
     * Intended for integration tests where all requests share the same IP
     * ({@code 127.0.0.1}) and bucket state would otherwise bleed between test methods.
     */
    public void clearBuckets() {
        authBuckets.clear();
        reviewBuckets.clear();
    }

    // ── Bucket factories ──────────────────────────────────────────────────────

    private static Bucket newAuthBucket() {
        // 20 requests per 10 minutes: generous for legitimate retries,
        // tight enough to stop automated credential-stuffing loops.
        return Bucket.builder()
            .addLimit(Bandwidth.classic(20, Refill.intervally(20, Duration.ofMinutes(10))))
            .build();
    }

    private static Bucket newReviewBucket() {
        // 5 requests per hour: a real user won't post more than 5 reviews in
        // an hour; a spam bot hits this on the first burst.
        // Note: the moderation queue remains the primary defence — see ReviewController.
        return Bucket.builder()
            .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofHours(1))))
            .build();
    }

    // ── Filter logic ──────────────────────────────────────────────────────────

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        Bucket bucket = resolveBucket(request);

        if (bucket != null) {
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            if (!probe.isConsumed()) {
                long retryAfter = Math.max(1L,
                    TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
                writeTooManyRequests(response, retryAfter);
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private Bucket resolveBucket(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) return null;

        String ip  = clientIp(request);
        String uri = request.getRequestURI();

        if (AUTH_SOCIAL_PATH.equals(uri)) {
            return authBuckets.computeIfAbsent(ip, k -> authBucketFactory.get());
        }
        if (uri.matches(REVIEWS_URI_REGEX)) {
            return reviewBuckets.computeIfAbsent(ip, k -> reviewBucketFactory.get());
        }
        return null;
    }

    /**
     * Extracts the real client IP from the request.
     * Takes the leftmost value of {@code X-Forwarded-For} (the original client)
     * or falls back to the TCP remote address (which is the BFF's address when
     * Next.js sits in front).
     */
    static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response, long retryAfterSeconds)
            throws IOException {
        String traceId = MDC.get(RequestIdFilter.MDC_KEY);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(),
            new ApiErrorResponse(
                "rate_limit_exceeded",
                "Too many requests. Retry after " + retryAfterSeconds + " seconds.",
                traceId != null ? traceId : UUID.randomUUID().toString()
            ));
    }
}
