package com.careflow.ai.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Idempotency-Key enforcement for mutating clinical endpoints.
 *
 * Contract (documented in docs/adr/0002-transactional-outbox.md):
 * <ul>
 *   <li>Opt-in: requests WITHOUT an Idempotency-Key header pass through
 *       untouched — existing clients keep working.</li>
 *   <li>A request WITH a key whose stored record is COMPLETED is replayed from
 *       the stored response (header {@code Idempotency-Replayed: true}) — the
 *       clinical write is NOT executed again.</li>
 *   <li>Key reuse against a DIFFERENT endpoint is rejected with 422.</li>
 *   <li>A concurrent duplicate of an in-flight key is rejected with 409.</li>
 *   <li>5xx responses are never stored for replay — the key is abandoned so a
 *       retry re-executes against a recovered server.</li>
 * </ul>
 *
 * Protected prefixes cover every mutating clinical surface: medication
 * administration and diagnosis/vital writes under {@code /api/clinical},
 * task mutations under {@code /api/tasks}, handoff creation under
 * {@code /api/shift-handoffs}, and timeline writes under {@code /api/patients}.
 */
@Component
public class IdempotencyKeyFilter extends OncePerRequestFilter {

    private static final String HEADER = "Idempotency-Key";
    private static final Set<String> PROTECTED_PREFIXES = Set.of(
            "/api/clinical/", "/api/shift-handoffs", "/api/tasks", "/api/patients/");

    private final IdempotencyKeyService idempotencyKeyService;
    private final ObjectMapper objectMapper;

    public IdempotencyKeyFilter(IdempotencyKeyService idempotencyKeyService,
                                ObjectMapper objectMapper) {
        this.idempotencyKeyService = idempotencyKeyService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.GET.matches(request.getMethod())
                || HttpMethod.HEAD.matches(request.getMethod())
                || HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        boolean protectedPath = PROTECTED_PREFIXES.stream()
                .anyMatch(request.getRequestURI()::startsWith);
        if (!protectedPath) {
            return true;
        }
        // No key = opt out (legacy clients). Enforcement-on-header keeps this
        // backward compatible while making retries safe for clients that send it.
        return request.getHeader(HEADER) == null || request.getHeader(HEADER).isBlank();
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String key = request.getHeader(HEADER).trim();
        String fingerprint = fingerprint(request);

        Optional<IdempotencyKey> existing = idempotencyKeyService.findActive(key);
        if (existing.isPresent()) {
            IdempotencyKey previous = existing.get();
            if (!fingerprint.equals(previous.getRequestFingerprint())) {
                writeError(response, 422,
                        "Idempotency-Key was already used for a different endpoint. "
                                + "Generate a new key per logical operation.");
                return;
            }
            if (IdempotencyKey.STATUS_COMPLETED.equals(previous.getStatus())
                    && previous.getResponseStatus() != null) {
                response.setStatus(previous.getResponseStatus());
                response.setContentType("application/json");
                response.setHeader("Idempotency-Replayed", "true");
                response.getWriter().write(
                        previous.getResponseBody() == null ? "" : previous.getResponseBody());
                return;
            }
            writeError(response, 409,
                    "A request with this Idempotency-Key is still in progress. "
                            + "Retry shortly with the same key.");
            return;
        }

        String endpoint = request.getMethod() + " " + request.getRequestURI();
        try {
            idempotencyKeyService.start(key, endpoint, fingerprint);
        } catch (IdempotencyKeyInFlightException e) {
            // Lost the insert race with a concurrent duplicate.
            writeError(response, 409, e.getMessage());
            return;
        }

        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(request, responseWrapper);
        } finally {
            int status = responseWrapper.getStatus();
            if (status >= 500) {
                // Transient server failure: never replay it. Abandon the key
                // so the client's retry re-executes the operation.
                idempotencyKeyService.abandon(key);
            } else {
                idempotencyKeyService.complete(
                        key,
                        status,
                        new String(responseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8));
            }
            responseWrapper.copyBodyToResponse();
        }
    }

    /** SHA-256 of method|URI|query — rejects key reuse across endpoints. */
    private String fingerprint(HttpServletRequest request) {
        String material = request.getMethod()
                + "|" + request.getRequestURI()
                + "|" + (request.getQueryString() == null ? "" : request.getQueryString());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(material.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private void writeError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status);
        body.put("error", status == 409 ? "Conflict" : "Unprocessable Entity");
        body.put("message", message);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
