package com.careflow.ai.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

/**
 * Resilient wrapper around the Cloudinary SDK (Phase 4 – Resilience).
 *
 * <p>Storage operations flow through the Resilience4j pipeline configured for the
 * {@code cloudinary} instance in {@code application.yml}, innermost to outermost:
 * <em>retry → circuit breaker → time limiter</em>. Identical semantics to
 * {@link ResilientAiClient} but for the Cloudinary {@code cloudinary} instance
 * (3 retries / 500 ms base, 20 s timeout).</p>
 *
 * <p>Exhausted failures surface as {@link FileStorageUnavailableException}, which
 * callers (e.g. {@code FileAttachmentService}) map to a graceful, retryable message
 * instead of a 500.</p>
 */
@Component
public class ResilientCloudinaryClient {

    private static final Logger log = LoggerFactory.getLogger(ResilientCloudinaryClient.class);
    private static final String INSTANCE = "cloudinary";

    private final Cloudinary delegate;
    private final Uploader uploader;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final TimeLimiter timeLimiter;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ExecutorService callExecutor = Executors.newFixedThreadPool(2);

    public ResilientCloudinaryClient(Cloudinary delegate,
                                     CircuitBreakerRegistry circuitBreakerRegistry,
                                     RetryRegistry retryRegistry,
                                     TimeLimiterRegistry timeLimiterRegistry) {
        this.delegate = delegate;
        this.uploader = delegate.uploader();
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(INSTANCE);
        this.retry = retryRegistry.retry(INSTANCE);
        this.timeLimiter = timeLimiterRegistry.timeLimiter(INSTANCE);
    }

    /**
     * Upload bytes to Cloudinary with circuit-breaker / retry / timeout protection.
     *
     * @throws FileStorageUnavailableException on exhausted failures
     */
    public Map<?, ?> upload(byte[] data, Map<String, Object> options) {
        return execute("upload", () -> invoke(() -> uploader.upload(data, options)));
    }

    /**
     * Destroy a Cloudinary asset with circuit-breaker / retry / timeout protection.
     *
     * @throws FileStorageUnavailableException on exhausted failures
     */
    public Map<?, ?> destroy(String publicId, Map<String, Object> options) {
        return execute("destroy", () -> invoke(() -> uploader.destroy(publicId, options)));
    }

    /** Cloudinary SDK calls throw checked {@link IOException}; bridge to runtime. */
    private static Map<?, ?> invoke(CloudinaryCall call) {
        try {
            return call.run();
        } catch (IOException ex) {
            throw new RuntimeException("Cloudinary SDK call failed: " + ex.getMessage(), ex);
        }
    }

    private Map<?, ?> execute(String operation, Supplier<Map<?, ?>> call) {
        Supplier<CompletionStage<Map<?, ?>>> callStage =
                () -> CompletableFuture.supplyAsync(call, callExecutor);

        // Outermost → innermost: retry around the raw async call, then circuit
        // breaker, then the time limiter around the whole composed invocation.
        Supplier<CompletionStage<Map<?, ?>>> retried =
                Retry.decorateCompletionStage(retry, scheduler, callStage);
        Supplier<CompletionStage<Map<?, ?>>> cbProtected =
                CircuitBreaker.decorateCompletionStage(circuitBreaker, retried);

        try {
            return timeLimiter.executeCompletionStage(scheduler, cbProtected)
                    .toCompletableFuture().join();
        } catch (Exception ex) {
            Throwable root = rootCause(ex);
            log.warn("Cloudinary {} failed (circuit open, retries exhausted, or timeout): {}",
                    operation, root.getMessage());
            throw new FileStorageUnavailableException(
                    "File storage unavailable: " + root.getMessage(), ex);
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

    @FunctionalInterface
    private interface CloudinaryCall {
        Map<?, ?> run() throws IOException;
    }
}