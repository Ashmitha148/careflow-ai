package com.careflow.ai.config;

/**
 * Thrown when the AI service (Gemini) is unavailable — circuit open, retries
 * exhausted, or timed out. Callers catch this to surface a graceful
 * "AI summary unavailable" response with the raw clinical data, rather than
 * propagating a 500.
 */
public class AiUnavailableException extends RuntimeException {

    public AiUnavailableException(String message) {
        super(message);
    }

    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}