package com.careflow.ai.resilience;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that assigns a correlation-ID to every inbound HTTP request.
 *
 * <p>The ID is taken from the {@code X-Correlation-Id} request header when
 * present (allowing upstream load-balancers / API gateways to stamp their own
 * trace IDs), or minted as a new UUID if the header is absent.
 *
 * <p>It is:
 * <ul>
 *   <li>Placed in the SLF4J MDC under the key {@code "correlationId"} for the
 *       entire duration of the request (including async dispatch), so every log
 *       line emitted on the same thread carries the same trace token.</li>
 *   <li>Echoed back to the caller in the {@code X-Correlation-Id} response
 *       header, enabling end-to-end tracing from client to log aggregator.</li>
 *   <li>Already propagated into the transactional outbox rows via
 *       {@code OutboxService.enqueue} (which reads {@code MDC.get("correlationId")}),
 *       so clinical events carry the trace token that created them.</li>
 * </ul>
 *
 * <p>Runs before Spring Security ({@code @Order(1)}) so even auth failures are
 * tagged in the logs.
 */
@Component
@Order(1)
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Correlation-Id";
    private static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String correlationId = request.getHeader(HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
