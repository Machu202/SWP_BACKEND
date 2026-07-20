package com.mangastudio.backend.service;

import com.mangastudio.backend.dto.response.DirectChatMessageResponse;

import java.util.List;

public interface DirectChatService {
    List<DirectChatMessageResponse> getMessages(Long currentUserId, Long otherUserId);
    DirectChatMessageResponse sendMessage(Long senderId, Long recipientId, String content);
}
