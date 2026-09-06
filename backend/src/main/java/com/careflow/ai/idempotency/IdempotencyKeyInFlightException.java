package com.careflow.ai.idempotency;

/**
 * Thrown when a concurrent request presents an Idempotency-Key that is already
 * registered and still PROCESSING — the filter maps this to HTTP 409 instead
 * of executing the clinical write twice.
 */
public class IdempotencyKeyInFlightException extends RuntimeException {

    public IdempotencyKeyInFlightException(String idemKey) {
        super("Idempotency-Key '" + idemKey
                + "' is already in progress; retry after the original request completes");
    }
}