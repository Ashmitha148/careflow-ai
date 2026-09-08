package com.careflow.ai.service;

/** Thrown when a refresh token is expired. */
public class RefreshTokenExpiredException extends RuntimeException {
    public RefreshTokenExpiredException(String message) { super(message); }
}