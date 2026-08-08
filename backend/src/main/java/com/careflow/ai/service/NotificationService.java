package com.careflow.ai.service;

import com.careflow.ai.entity.Notification;
import com.careflow.ai.entity.NotificationType;
import com.careflow.ai.entity.User;
import com.careflow.ai.repository.NotificationRepository;
import com.careflow.ai.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
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

        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotifications(UUID userId) {
        return notificationRepository
                .findByUser_IdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(UUID userId) {
        return notificationRepository
                .findByUser_IdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository
                .countByUser_IdAndReadFalse(userId);
    }

    @Transactional
    public Notification markAsRead(UUID notificationId, UUID userId) {

        Notification notification = notificationRepository
                .findById(notificationId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Notification not found: " + notificationId));

        if (notification.getUser() == null
                || !notification.getUser().getId().equals(userId)) {
            throw new IllegalStateException(
                    "You can only modify your own notifications");
        }

        notification.setRead(true);

        return notificationRepository.save(notification);
    }

    @Transactional
    public int markAllAsRead(UUID userId) {

        List<Notification> notifications =
                notificationRepository
                        .findByUser_IdAndReadFalseOrderByCreatedAtDesc(userId);

        for (Notification notification : notifications) {
            notification.setRead(true);
        }

        notificationRepository.saveAll(notifications);

        return notifications.size();
    }
}
