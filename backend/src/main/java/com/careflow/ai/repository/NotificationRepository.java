package com.careflow.ai.repository;

import com.careflow.ai.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    List<Notification> findByUser_IdAndReadFalseOrderByCreatedAtDesc(UUID userId);

    long countByUser_IdAndReadFalse(UUID userId);
}
