package com.careflow.ai.service;

/** Thrown when a refresh token's family has been revoked. */
public class RefreshTokenRevokedException extends RuntimeException {
    public RefreshTokenRevokedException(String message) { super(message); }
}