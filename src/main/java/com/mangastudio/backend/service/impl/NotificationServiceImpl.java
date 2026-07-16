package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.entity.Notification;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.NotificationRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.NotificationService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   UserRepository userRepository,
                                   SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    @Transactional
    public Notification createNotification(Long userId, String message) {
        return createNotification(userId, message, null);
    }

    @Override
    @Transactional
    public Notification createNotification(Long userId, String message, String actionUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Error: User not found"));

        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .actionUrl(actionUrl)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification savedNotification = notificationRepository.save(notification);
        messagingTemplate.convertAndSend("/topic/notifications/" + userId, savedNotification);
        return savedNotification;
    }

    @Override
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional
    public Notification markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Error: Notification not found"));
        if (notification.getUser() == null || !notification.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You cannot update another user's notification.");
        }
        notification.setIsRead(true);
        return notificationRepository.save(notification);
    }
}
