package com.mangastudio.backend.dto.response;

import java.time.LocalDateTime;

public class BoardChatMessageResponse {
    private final Long id;
    private final Long seriesId;
    private final Long senderId;
    private final String senderName;
    private final String content;
    private final LocalDateTime createdAt;

    public BoardChatMessageResponse(Long id, Long seriesId, Long senderId, String senderName,
                                    String content, LocalDateTime createdAt) {
        this.id = id;
        this.seriesId = seriesId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.content = content;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getSeriesId() { return seriesId; }
    public Long getSenderId() { return senderId; }
    public String getSenderName() { return senderName; }
    public String getContent() { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
