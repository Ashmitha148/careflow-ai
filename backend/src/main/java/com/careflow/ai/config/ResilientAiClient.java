package com.careflow.ai.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

/**
 * Resilient wrapper around {@link GeminiClient} (Phase 4 – Resilience).
 *
 * <p>Every AI call flows through the Resilience4j pipeline configured for the
 * {@code gemini} instance in {@code application.yml}, applied innermost to
 * outermost: <em>retry → circuit breaker → time limiter</em>:
 * <ul>
 *   <li><strong>Retry</strong> — transient Gemini failures are retried (3 attempts,
 *       exponential backoff from 500 ms). The whole retry chain is one circuit-breaker
 *       call, so each composed invocation counts once against the breaker.</li>
 *   <li><strong>Circuit breaker</strong> — after repeated failures the {@code gemini}
 *       breaker opens and calls fail fast with {@code CallNotPermittedException}
 *       (no slow request stuck behind a dead upstream).</li>
 *   <li><strong>Time limiter</strong> — a call that has not completed within 30 s is
 *       cancelled and reported as unavailable.</li>
 * </ul>
 *
 * <p>All three registries are auto-configured by {@code resilience4j-spring-boot3}
 * straight from {@code application.yml} — it remains the single source of truth and
 * no programmatic {@code @Bean} duplicates exist.</p>
 *
 * <p>Any exhausted failure mode is surfaced to callers (e.g. {@code CopilotService})
 * as an {@link AiUnavailableException}, which they map to a graceful "AI summary
 * unavailable" response instead of a 500.</p>
 */
@Component
public class ResilientAiClient {

    private static final Logger log = LoggerFactory.getLogger(ResilientAiClient.class);
    private static final String INSTANCE = "gemini";

    private final GeminiClient delegate;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final TimeLimiter timeLimiter;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ExecutorService callExecutor = Executors.newFixedThreadPool(2);

    public ResilientAiClient(GeminiClient delegate,
                             CircuitBreakerRegistry circuitBreakerRegistry,
                             RetryRegistry retryRegistry,
                             TimeLimiterRegistry timeLimiterRegistry) {
        this.delegate = delegate;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(INSTANCE);
        this.retry = retryRegistry.retry(INSTANCE);
        this.timeLimiter = timeLimiterRegistry.timeLimiter(INSTANCE);
    }

    /**
     * Generate a Gemini completion with circuit-breaker / retry / timeout protection.
     *
     * @param prompt the prompt to send to Gemini
     * @return the generated text
     * @throws AiUnavailableException if the circuit is open, retries are exhausted,
     *         or the call times out — <em>never</em> a raw infrastructure exception
     */
    public String generate(String prompt) {
        Supplier<CompletionStage<String>> call =
                () -> CompletableFuture.supplyAsync(() -> delegate.generate(prompt), callExecutor);

        // Outermost → innermost: retry around the raw async call, then circuit
        // breaker, then the time limiter around the whole composed invocation.
        Supplier<CompletionStage<String>> retried =
                Retry.decorateCompletionStage(retry, scheduler, call);
        Supplier<CompletionStage<String>> cbProtected =
                CircuitBreaker.decorateCompletionStage(circuitBreaker, retried);

        try {
            return timeLimiter.executeCompletionStage(scheduler, cbProtected)
                    .toCompletableFuture().join();
        } catch (Exception ex) {
            Throwable root = rootCause(ex);
            if (circuitBreaker.getState() == CircuitBreaker.State.OPEN) {
                log.warn("Gemini circuit is OPEN — skipping AI call: {}", root.getMessage());
            } else {
                log.warn("Gemini call failed (retries exhausted or timeout): {}", root.getMessage());
            }
            throw new AiUnavailableException("AI service unavailable: " + root.getMessage(), ex);
        }
    }

    /** Unwraps CompletionException/ExecutionException layers to the real cause. */
    private static Throwable rootCause(Throwable t) {
        Throwable cause = t;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException)
                && cause.getCause() != null
                && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
        callExecutor.shutdownNow();
    }
}