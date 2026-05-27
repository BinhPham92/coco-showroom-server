package com.cocoshowroom.server.shared;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Generates a unique request ID for every inbound HTTP request.
 *
 * <p>The ID is:
 * <ul>
 *   <li>Written into the SLF4J MDC under {@value MDC_KEY} so it appears in
 *       every structured log line emitted during the request lifecycle.</li>
 *   <li>Echoed back to the caller as an {@code X-Request-Id} response header
 *       so clients can correlate their logs with server logs.</li>
 * </ul>
 *
 * <p>Runs at {@link Ordered#HIGHEST_PRECEDENCE} so all downstream filters and
 * handler code see the MDC key already populated.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    /** MDC key; referenced by {@link RateLimitingFilter} and {@link GlobalExceptionHandler}. */
    public static final String MDC_KEY         = "requestId";
    public static final String RESPONSE_HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String id = UUID.randomUUID().toString();
        MDC.put(MDC_KEY, id);
        response.setHeader(RESPONSE_HEADER, id);
        try {
            chain.doFilter(request, response);
        } finally {
            // Must remove to prevent MDC leak when the thread is returned to the pool.
            MDC.remove(MDC_KEY);
        }
    }
}
