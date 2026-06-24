package com.mangastudio.backend.controller;

import com.mangastudio.backend.entity.Notification;
import com.mangastudio.backend.security.UserDetailsImpl;
import com.mangastudio.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // Lấy tất cả thông báo chưa đọc của bản thân
    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> getMyUnreadNotifications(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<Notification> notifications = notificationService.getUnreadNotifications(userDetails.getId());
        return ResponseEntity.ok(notifications);
    }

    // Đánh dấu 1 thông báo là đã đọc
    @PatchMapping("/{id}/read")
    public ResponseEntity<Notification> markNotificationAsRead(@PathVariable Long id) {
        Notification updatedNotification = notificationService.markAsRead(id);
        return ResponseEntity.ok(updatedNotification);
    }
}