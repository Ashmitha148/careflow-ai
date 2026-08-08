package com.careflow.ai.controller;

import com.careflow.ai.entity.AuditLog;
import com.careflow.ai.service.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogService.getAuditLogs(pageable);
    }

    @GetMapping("/user/{userId}")
    public Page<AuditLog> getAuditLogsByUser(
            @PathVariable UUID userId,
            Pageable pageable) {
        return auditLogService.getAuditLogsByUser(
                userId, pageable);
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public Page<AuditLog> getAuditLogsByEntity(
            @PathVariable String entityType,
            @PathVariable UUID entityId,
            Pageable pageable) {
        return auditLogService.getAuditLogsByEntity(
                entityType, entityId, pageable);
    }
}
