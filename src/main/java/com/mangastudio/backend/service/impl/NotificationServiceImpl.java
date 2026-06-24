package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.entity.Notification;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.NotificationRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    
    // [BỔ SUNG FE-09] Cỗ máy chuyên dụng để gửi tin nhắn WebSocket
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public Notification createNotification(Long userId, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Error: User not found"));

        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        // 1. Lưu thông báo vào Database
        Notification savedNotification = notificationRepository.save(notification);

        // 2. [FE-09] Bắn thông báo Real-time đích danh tới User ID
        // Frontend sẽ lắng nghe ở kênh: /topic/notifications/{userId}
        String destination = "/topic/notifications/" + userId;
        messagingTemplate.convertAndSend(destination, savedNotification);
        
        System.out.println(">>> [WEBSOCKET] Fired Real-time notification: " + destination);

        return savedNotification;
    }

    @Override
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findAll().stream()
                .filter(notif -> notif.getUser().getId().equals(userId) && !notif.getIsRead())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Error: Notification not found"));

        notification.setIsRead(true);
        return notificationRepository.save(notification);
    }
}