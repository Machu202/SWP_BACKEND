package com.mangastudio.backend.service;

import com.mangastudio.backend.dto.response.BoardChatMessageResponse;

import java.util.List;

public interface BoardChatService {
    List<BoardChatMessageResponse> getMessages(Long seriesId, Long currentUserId);
    BoardChatMessageResponse sendMessage(Long seriesId, Long boardMemberId, String content);
}
