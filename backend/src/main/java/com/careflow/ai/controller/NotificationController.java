package com.careflow.ai.controller;

import com.careflow.ai.entity.Notification;
import com.careflow.ai.entity.NotificationType;
import com.careflow.ai.service.NotificationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public List<Notification> getNotifications(
            @PathVariable UUID userId) {
        return notificationService.getNotifications(userId);
    }

    @GetMapping("/user/{userId}/unread")
    @PreAuthorize("isAuthenticated()")
    public List<Notification> getUnreadNotifications(
            @PathVariable UUID userId) {
        return notificationService.getUnreadNotifications(userId);
    }

    @GetMapping("/user/{userId}/unread/count")
    @PreAuthorize("isAuthenticated()")
    public long getUnreadCount(
            @PathVariable UUID userId) {
        return notificationService.getUnreadCount(userId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('DOCTOR','NURSE','ADMIN')")
    public Notification createNotification(
            @RequestParam UUID userId,
            @RequestParam NotificationType type,
            @RequestParam String message) {
        return notificationService.createNotification(
                userId, type, message);
    }

    @PatchMapping("/{notificationId}/read")
    @PreAuthorize("isAuthenticated()")
    public Notification markAsRead(
            @PathVariable UUID notificationId,
            @RequestParam UUID userId) {
        return notificationService.markAsRead(
                notificationId, userId);
    }

    @PostMapping("/user/{userId}/read-all")
    @PreAuthorize("isAuthenticated()")
    public int markAllAsRead(
            @PathVariable UUID userId) {
        return notificationService.markAllAsRead(userId);
    }
}
