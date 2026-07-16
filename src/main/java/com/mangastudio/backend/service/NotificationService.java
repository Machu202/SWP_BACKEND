package com.mangastudio.backend.service;

import com.mangastudio.backend.entity.Notification;
import java.util.List;

public interface NotificationService {
    Notification createNotification(Long userId, String message);
    Notification createNotification(Long userId, String message, String actionUrl);
    List<Notification> getUnreadNotifications(Long userId);
    Notification markAsRead(Long notificationId, Long userId);
}
