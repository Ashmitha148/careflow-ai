package com.careflow.ai.outbox;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables the scheduler for the outbox relay, the idempotency-key purge, and
 * the async AI workers. Kept as its own configuration so the scheduling
 * decision is visible next to the components that depend on it.
 */
@Configuration
@EnableScheduling
public class OutboxSchedulingConfig {
}
