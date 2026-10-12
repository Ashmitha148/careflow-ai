package com.careflow.ai.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {

        return buildResponse(HttpStatus.FORBIDDEN, "FORBIDDEN",
                "You do not have permission to perform this action.", request);
    }

    @ExceptionHandler(com.careflow.ai.security.PatientAccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handlePatientAccessDeniedException(
            com.careflow.ai.security.PatientAccessDeniedException ex, HttpServletRequest request) {

        return buildResponse(HttpStatus.FORBIDDEN, "PATIENT_ACCESS_DENIED", ex.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {

        return buildResponse(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ex.getMessage(), request);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFoundException(
            EntityNotFoundException ex, HttpServletRequest request) {

        return buildResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.getMessage(), request);
    }

    @ExceptionHandler({OptimisticLockException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<Map<String, Object>> handleOptimisticLockException(
            Exception ex, HttpServletRequest request) {

        log.warn("Optimistic lock conflict: {}", ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION_CONFLICT",
                "This record was updated concurrently by another user. Please reload the latest changes.", request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(
            IllegalStateException ex, HttpServletRequest request) {

        return buildResponse(HttpStatus.CONFLICT, "INVALID_STATE", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {

        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", ex.getMessage(), request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParameterException(
            MissingServletRequestParameterException ex, HttpServletRequest request) {

        return buildResponse(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER",
                "Required parameter '" + ex.getParameterName() + "' is missing", request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleNotReadableException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        return buildResponse(HttpStatus.BAD_REQUEST, "MALFORMED_JSON",
                "Malformed JSON request body.", request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.toList());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("code", "VALIDATION_FAILED");
        body.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
        body.put("message", "Validation failed for one or more fields");
        body.put("path", request != null ? request.getRequestURI() : null);
        body.put("traceId", MDC.get("correlationId"));
        body.put("fieldErrors", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex, HttpServletRequest request) {

        // Check for common not found messages in legacy service code
        if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("not found")) {
            return buildResponse(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), request);
        }

        return buildResponse(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(
            Exception ex, HttpServletRequest request) {

        log.error("Unhandled exception processing request: {}", request != null ? request.getRequestURI() : "unknown", ex);

        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR",
                "An unexpected server error occurred.", request);
    }

    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("code", code);
        body.put("error", status.getReasonPhrase());
        body.put("message", message != null ? message : status.getReasonPhrase());
        body.put("path", request != null ? request.getRequestURI() : null);
        body.put("traceId", MDC.get("correlationId"));

        return ResponseEntity.status(status).body(body);
    }
}
