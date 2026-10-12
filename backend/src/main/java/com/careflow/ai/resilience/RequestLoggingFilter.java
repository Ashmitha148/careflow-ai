package com.careflow.ai.resilience;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Emits a structured access-log entry for every HTTP request (method, path,
 * status, duration in ms) at INFO level using SLF4J.
 *
 * <p>The log line automatically includes the {@code correlationId} MDC key
 * stamped by {@link CorrelationIdFilter} (which runs at Order 1, before this
 * filter at Order 2), enabling the full request trace to be reconstructed from
 * any log aggregator by searching for the correlation ID.
 *
 * <p>Actuator scrape endpoints ({@code /actuator/**}) are suppressed so they
 * don't flood the log in production (Prometheus scrapes every 15 s by default).
 */
@Component
@Order(2)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("careflow.access");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            String uri = request.getRequestURI();
            // Skip actuator scrapes to avoid log pollution
            if (!uri.startsWith("/api/actuator") && !uri.startsWith("/actuator")) {
                long durationMs = System.currentTimeMillis() - start;
                log.info("method={} uri={} status={} durationMs={} correlationId={}",
                        request.getMethod(),
                        uri,
                        response.getStatus(),
                        durationMs,
                        MDC.get("correlationId"));
            }
        }
    }
}
