package com.careflow.ai.service;

/**
 * Thrown when a refresh token that has ALREADY been used is presented again.
 * This signals possible token theft — the entire token family is revoked.
 */
public class RefreshTokenReuseException extends RuntimeException {
    public RefreshTokenReuseException(String message) { super(message); }
}