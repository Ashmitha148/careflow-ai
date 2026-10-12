package com.careflow.ai.repository;

import com.careflow.ai.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);

    Page<AuditLog> findByTimestampLessThanOrderByTimestampDesc(java.time.LocalDateTime timestamp, Pageable pageable);

    Page<AuditLog> findByUser_Hospital_IdOrderByTimestampDesc(UUID hospitalId, Pageable pageable);

    Page<AuditLog> findByUser_Hospital_IdAndTimestampLessThanOrderByTimestampDesc(
            UUID hospitalId, java.time.LocalDateTime timestamp, Pageable pageable);

    Page<AuditLog> findByUser_IdOrderByTimestampDesc(UUID userId, Pageable pageable);

    Page<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(
            String entityType, UUID entityId, Pageable pageable);
}
