package com.careflow.ai.resilience;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter filter for sensitive endpoints (Phase 4 – Resilience).
 *
 * <p>Uses <strong>Bucket4j</strong> token-bucket limiting, keyed per client IP so a
 * single rogue caller cannot starve the API for everyone else. The budgets read the
 * same {@code limit-for-period} values that the Resilience4j property block in
 * {@code application.yml} exposes (env-overridable via {@code RATE_LIMIT_AUTH} and
 * {@code RATE_LIMIT_AI}):</p>
 *
 * <ul>
 *   <li>POST {@code /auth/login} and {@code /auth/refresh} → auth bucket (default 10
 *       requests / min) to slow credential-stuffing.</li>
 *   <li>{@code /copilot/**} and {@code /ai/**} → AI bucket (default 30 / min).</li>
 * </ul>
 *
 * <p>Buckets are created lazily per IP and cached in {@link ConcurrentHashMap}s —
 * no memory is spent until an IP actually calls a protected endpoint.</p>
 *
 * <p>Disabled in the {@code dev} profile used by tests (all test requests share a
 * single container IP and the tight auth budget would block legitimate logins);
 * enforced in prod via the {@code prod} profile.</p>
 */
@Component
@Profile("!dev")
@Order(10)
public class RateLimitingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final int authCapacity;
    private final int aiCapacity;
    private final ConcurrentHashMap<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> aiBuckets = new ConcurrentHashMap<>();

    public RateLimitingFilter(
            @Value("${resilience4j.ratelimiter.instances.auth.limit-for-period:10}")
            int authCapacity,
            @Value("${resilience4j.ratelimiter.instances.ai.limit-for-period:30}")
            int aiCapacity) {
        this.authCapacity = authCapacity;
        this.aiCapacity = aiCapacity;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();
        String ip = clientIp(httpRequest);

        Bucket bucket = null;

        if ("POST".equals(method)
                && (path.endsWith("/auth/login") || path.endsWith("/auth/refresh"))) {
            bucket = authBuckets.computeIfAbsent(ip, this::authBucket);
        } else if (path.contains("/copilot/") || path.contains("/ai/")) {
            bucket = aiBuckets.computeIfAbsent(ip, this::aiBucket);
        }

        if (bucket != null && !bucket.tryConsume(1)) {
            log.warn("Rate limit exceeded for IP {} on {} {}", ip, method, path);
            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write(
                    "{\"error\":\"Rate limit exceeded. Please try again later.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    private Bucket authBucket(String ip) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(
                        authCapacity,
                        Refill.greedy(authCapacity, Duration.ofMinutes(1))))
                .build();
    }

    private Bucket aiBucket(String ip) {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(
                        aiCapacity,
                        Refill.greedy(aiCapacity, Duration.ofMinutes(1))))
                .build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}