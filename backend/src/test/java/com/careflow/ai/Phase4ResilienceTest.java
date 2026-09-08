package com.careflow.ai;

import com.careflow.ai.config.AiUnavailableException;
import com.careflow.ai.config.GeminiClient;
import com.careflow.ai.config.ResilientAiClient;
import com.careflow.ai.resilience.RateLimitingFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 4 — Resilience integration tests.
 *
 * Verifies:
 * 1. The Resilience4j circuit breaker (YAML-configured {@code gemini} instance)
 *    opens after repeated failures, the retry chain is capped at
 *    {@code max-attempts}, and an open circuit fails FAST without invoking the
 *    delegate.
 * 2. The Bucket4j {@link RateLimitingFilter} returns 429 on burst, is keyed per
 *    client IP, and leaves non-protected requests untouched.
 * 3. Refresh-token rotation issues a fresh pair and marks the old token used.
 * 4. Refresh-token reuse detection revokes the ENTIRE token family.
 *
 * Retry wait-duration is collapsed to 10ms via test properties so the five
 * failure rounds complete quickly; every other knob is production YAML.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        // speed up the retry chain; max-attempts stays at the production value 3
        "resilience4j.retry.instances.gemini.wait-duration=10ms",
})
public class Phase4ResilienceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ResilientAiClient resilientAiClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @MockBean
    private GeminiClient geminiClient;

    /** Seeded by V2/V5 Flyway migrations in every fresh test database. */
    private static final String SEED_NURSE_EMAIL = "nurse1@careflow.ai";

    // -------------------------------------------------------------------------
    // 1. Circuit breaker + retry (gemini instance)
    // -------------------------------------------------------------------------

    /**
     * Five consecutive failures (≥ minimum-number-of-calls, failure rate 100%
     * ≥ threshold 50%) must OPEN the breaker; each round must retry exactly
     * {@code max-attempts} times; a call against the OPEN circuit must fail
     * fast without reaching the delegate.
     */
    @Test
    void circuitBreakerOpensOnRepeatedAiFailuresAndFailsFast() {
        when(geminiClient.generate(any(String.class)))
                .thenThrow(new IllegalStateException("gemini is down"));

        // YAML: minimum-number-of-calls=5, failure-rate-threshold=50 → OPEN after 5
        for (int i = 0; i < 5; i++) {
            assertThrows(AiUnavailableException.class,
                    () -> resilientAiClient.generate("prompt"),
                    "exhausted retries must surface as AiUnavailableException");
        }

        CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker("gemini");
        assertEquals(CircuitBreaker.State.OPEN, breaker.getState(),
                "5/5 failures must trip the gemini circuit breaker");

        // Each round retried exactly max-attempts (3) times → 5 × 3 delegate calls
        verify(geminiClient, times(15)).generate(any(String.class));

        // While OPEN: fail fast — the delegate must NOT be invoked again
        assertThrows(AiUnavailableException.class,
                () -> resilientAiClient.generate("prompt"));
        verify(geminiClient, times(15)).generate(any(String.class));
    }

    // -------------------------------------------------------------------------
    // 2. Bucket4j rate-limiting filter
    // -------------------------------------------------------------------------

    /** Bursting past the auth budget gets 429; another IP is unaffected. */
    @Test
    void rateLimitingFilterReturns429OnAuthBurstPerIp() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(3, 30);

        for (int i = 0; i < 3; i++) {
            AtomicBoolean reachedChain = new AtomicBoolean(false);
            MockHttpServletResponse res = runFilter(filter,
                    "POST", "/api/auth/login", "10.0.0.1", reachedChain);
            assertEquals(200, res.getStatus(), "request " + i + " must pass");
            assertTrue(reachedChain.get(), "request " + i + " must reach the chain");
        }

        // 4th request from the same IP: bucket empty → 429, chain never invoked
        AtomicBoolean reachedChain = new AtomicBoolean(false);
        MockHttpServletResponse blocked = runFilter(filter,
                "POST", "/api/auth/login", "10.0.0.1", reachedChain);
        assertEquals(429, blocked.getStatus());
        assertTrue(blocked.getContentAsString().contains("Rate limit exceeded"));
        assertFalse(reachedChain.get(), "rate-limited request must never reach the chain");

        // A different IP has its own bucket and still passes
        AtomicBoolean otherIpChain = new AtomicBoolean(false);
        MockHttpServletResponse otherIp = runFilter(filter,
                "POST", "/api/auth/login", "10.0.0.2", otherIpChain);
        assertEquals(200, otherIp.getStatus());
        assertTrue(otherIpChain.get());
    }

    /** AI endpoints get their own budget; unprotected paths are never limited. */
    @Test
    void rateLimitingFilterSeparatesAiBucketAndIgnoresGets() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter(10, 2);

        for (int i = 0; i < 2; i++) {
            MockHttpServletResponse res = runFilter(filter,
                    "POST", "/api/copilot/chat", "10.1.0.1", new AtomicBoolean(false));
            assertEquals(200, res.getStatus(), "AI request " + i + " must pass");
        }
        assertEquals(429, runFilter(filter,
                "POST", "/api/copilot/chat", "10.1.0.1", new AtomicBoolean(false)).getStatus(),
                "3rd AI request from same IP must be limited");

        // Unprotected endpoints (no /auth/login, /auth/refresh, /copilot/, /ai/)
        // are never limited, regardless of method
        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse res = runFilter(filter,
                    "GET", "/api/patients", "10.1.0.1", new AtomicBoolean(false));
            assertEquals(200, res.getStatus(), "unprotected request must never be limited");
        }
    }

    private MockHttpServletResponse runFilter(RateLimitingFilter filter,
                                              String method, String path, String ip,
                                              AtomicBoolean reachedChain) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> reachedChain.set(true);
        filter.doFilter(request, response, chain);
        return response;
    }

    // -------------------------------------------------------------------------
    // 3 + 4. Refresh-token rotation and reuse detection
    // -------------------------------------------------------------------------

    /** Rotation swaps both tokens; the presented token cannot be replayed. */
    @Test
    void refreshTokenRotationIssuesNewPairAndOldTokenIsRejected() throws Exception {
        String originalRefresh = login(SEED_NURSE_EMAIL);

        // Rotate: present the original refresh token, get a fresh pair
        MvcResult rotated = mockMvc.perform(post("/api/auth/refresh")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Collections.singletonMap("refreshToken", originalRefresh))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(rotated.getResponse().getContentAsString());
        String newAccess = body.get("token").asText();
        String newRefresh = body.get("refreshToken").asText();

        assertNotEquals(originalRefresh, newRefresh,
                "rotation must issue a new refresh token");
        assertFalse(newAccess.isBlank(), "rotation must issue a new access token");

        // The ORIGINAL token was marked used — replaying it must be rejected
        mockMvc.perform(post("/api/auth/refresh")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Collections.singletonMap("refreshToken", originalRefresh))))
                .andExpect(status().isBadRequest());
    }

    /** Replaying an already-used refresh token revokes the whole family. */
    @Test
    void refreshTokenReuseRevokesEntireFamily() throws Exception {
        String originalRefresh = login(SEED_NURSE_EMAIL);

        // Legitimate rotation → generation-2 token for the same family
        MvcResult rotated = mockMvc.perform(post("/api/auth/refresh")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Collections.singletonMap("refreshToken", originalRefresh))))
                .andExpect(status().isOk())
                .andReturn();
        String gen2Refresh = objectMapper.readTree(
                rotated.getResponse().getContentAsString()).get("refreshToken").asText();

        // THEFT SIGNAL: the used generation-1 token is replayed
        mockMvc.perform(post("/api/auth/refresh")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Collections.singletonMap("refreshToken", originalRefresh))))
                .andExpect(status().isBadRequest());

        // The whole family is revoked — even the legitimate generation-2 token
        mockMvc.perform(post("/api/auth/refresh")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Collections.singletonMap("refreshToken", gen2Refresh))))
                .andExpect(status().isBadRequest()); // family revocation kills every token
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(objectMapper.createObjectNode()
                                .put("email", email)
                                .put("password", "password123"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("refreshToken").asText();
    }
}