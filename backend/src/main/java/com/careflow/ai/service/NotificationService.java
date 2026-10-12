package com.careflow.ai.service;

import com.careflow.ai.entity.Notification;
import com.careflow.ai.entity.NotificationType;
import com.careflow.ai.entity.User;
import com.careflow.ai.repository.NotificationRepository;
import com.careflow.ai.repository.UserRepository;
import com.careflow.ai.websocket.RealtimeEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            RealtimeEventPublisher realtimeEventPublisher) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    @Transactional
    public Notification createNotification(
            UUID userId,
            NotificationType type,
            String message) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found: " + userId));

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setType(type);
        notification.setMessage(message);
        notification.setRead(false);

        Notification saved = notificationRepository.save(notification);

        realtimeEventPublisher.publishNotification(userId, saved);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotifications(UUID userId) {
        ensureSelfOrAdmin(userId);
        return notificationRepository
                .findByUser_IdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(UUID userId) {
        ensureSelfOrAdmin(userId);
        return notificationRepository
                .findByUser_IdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        ensureSelfOrAdmin(userId);
        return notificationRepository
                .countByUser_IdAndReadFalse(userId);
    }

    @Transactional
    public Notification markAsRead(UUID notificationId, UUID userId) {

        ensureSelfOrAdmin(userId);
        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Notification not found: " + notificationId));

        if (notification.getUser() == null
                || !notification.getUser().getId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You can only modify your own notifications");
        }

        notification.setRead(true);

        return notificationRepository.save(notification);
    }

    @Transactional
    public int markAllAsRead(UUID userId) {

        ensureSelfOrAdmin(userId);
        List<Notification> notifications =
                notificationRepository
                        .findByUser_IdAndReadFalseOrderByCreatedAtDesc(userId);

        for (Notification notification : notifications) {
            notification.setRead(true);
        }

        notificationRepository.saveAll(notifications);

        return notifications.size();
    }

    /** Users may read and mutate only their own notifications; admins may act for any. */
    private void ensureSelfOrAdmin(UUID userId) {
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof com.careflow.ai.security.CustomUserDetails actor)) {
            throw new RuntimeException("Authentication required");
        }
        if (actor.getUser().getRole() == com.careflow.ai.entity.Role.ADMIN
                || actor.getUser().getId().equals(userId)) {
            return;
        }
        throw new org.springframework.security.access.AccessDeniedException(
                "Not authorized to access another user's notifications");
    }
}
