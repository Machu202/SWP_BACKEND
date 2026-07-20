package com.mangastudio.backend.dto.response;

import java.time.LocalDateTime;

public class DirectChatMessageResponse {
    private final Long id;
    private final Long senderId;
    private final Long recipientId;
    private final String senderName;
    private final String recipientName;
    private final String content;
    private final LocalDateTime createdAt;

    public DirectChatMessageResponse(Long id, Long senderId, Long recipientId, String senderName,
                                     String recipientName, String content, LocalDateTime createdAt) {
        this.id = id;
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.senderName = senderName;
        this.recipientName = recipientName;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getSenderId() { return senderId; }
    public Long getRecipientId() { return recipientId; }
    public String getSenderName() { return senderName; }
    public String getRecipientName() { return recipientName; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
