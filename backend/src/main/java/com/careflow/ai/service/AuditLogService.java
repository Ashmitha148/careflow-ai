package com.careflow.ai.service;

import com.careflow.ai.dto.CursorPageResponse;
import com.careflow.ai.entity.AuditLog;
import com.careflow.ai.entity.User;
import com.careflow.ai.repository.AuditLogRepository;
import com.careflow.ai.repository.UserRepository;
import com.careflow.ai.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Sole writer of {@code audit_logs}. Other services must call {@link #log} rather than
 * persisting {@link AuditLog} entities directly.
 */
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    private User resolveCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails details) {
            return details.getUser();
        }
        return null;
    }

    /**
     * Internal write API for application services. Controllers should not call this directly
     * for arbitrary payloads; use domain services that compose meaningful audit entries.
     */
    @Transactional
    public AuditLog log(UUID userId, String action, String entityType, UUID entityId, String metadata) {
        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        }

        AuditLog entry = AuditLog.builder()
                .user(user)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .metadata(metadata)
                .build();

        return auditLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        User actor = resolveCurrentUser();
        if (actor != null && actor.getHospital() != null) {
            return auditLogRepository.findByUser_Hospital_IdOrderByTimestampDesc(actor.getHospital().getId(), pageable);
        }
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable);
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<AuditLog> getAuditLogsCursor(String cursor, int limit) {
        int pageSize = Math.min(Math.max(limit, 1), 100);
        User actor = resolveCurrentUser();
        Page<AuditLog> page;

        java.time.LocalDateTime cursorTimestamp = null;
        if (cursor != null && !cursor.isBlank()) {
            try {
                UUID cursorId = UUID.fromString(cursor.trim());
                AuditLog cursorLog = auditLogRepository.findById(cursorId).orElse(null);
                if (cursorLog != null) {
                    cursorTimestamp = cursorLog.getTimestamp();
                }
            } catch (IllegalArgumentException e) {
                // Ignore malformed cursor
            }
        }

        if (actor != null && actor.getHospital() != null) {
            if (cursorTimestamp != null) {
                page = auditLogRepository.findByUser_Hospital_IdAndTimestampLessThanOrderByTimestampDesc(
                        actor.getHospital().getId(), cursorTimestamp, PageRequest.of(0, pageSize + 1));
            } else {
                page = auditLogRepository.findByUser_Hospital_IdOrderByTimestampDesc(
                        actor.getHospital().getId(), PageRequest.of(0, pageSize + 1));
            }
        } else {
            if (cursorTimestamp != null) {
                page = auditLogRepository.findByTimestampLessThanOrderByTimestampDesc(
                        cursorTimestamp, PageRequest.of(0, pageSize + 1));
            } else {
                page = auditLogRepository.findAllByOrderByTimestampDesc(
                        PageRequest.of(0, pageSize + 1));
            }
        }
        List<AuditLog> all = page.getContent();
        boolean hasMore = all.size() > pageSize;
        List<AuditLog> items = hasMore ? all.subList(0, pageSize) : all;
        String nextCursor = (hasMore && !items.isEmpty()) ? items.get(items.size() - 1).getId().toString() : null;

        return CursorPageResponse.<AuditLog>builder()
                .items(items)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .pageSize(pageSize)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByUser(UUID userId, Pageable pageable) {
        return auditLogRepository.findByUser_IdOrderByTimestampDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByEntity(String entityType, UUID entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId, pageable);
    }
}
