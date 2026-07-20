package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.dto.response.BoardChatMessageResponse;
import com.mangastudio.backend.entity.BoardChatMessage;
import com.mangastudio.backend.entity.MangaSeries;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.BoardChatMessageRepository;
import com.mangastudio.backend.repository.MangaSeriesRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.BoardChatService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BoardChatServiceImpl implements BoardChatService {

    private static final int MAX_MESSAGE_LENGTH = 2000;

    private final BoardChatMessageRepository boardChatMessageRepository;
    private final MangaSeriesRepository mangaSeriesRepository;
    private final UserRepository userRepository;

    public BoardChatServiceImpl(BoardChatMessageRepository boardChatMessageRepository,
                                MangaSeriesRepository mangaSeriesRepository,
                                UserRepository userRepository) {
        this.boardChatMessageRepository = boardChatMessageRepository;
        this.mangaSeriesRepository = mangaSeriesRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoardChatMessageResponse> getMessages(Long seriesId, Long currentUserId) {
        User currentUser = requireUser(currentUserId);
        String role = roleName(currentUser);
        if (!isBoard(role) && !isAdmin(role)) {
            throw new AccessDeniedException("Only Editorial Board members and Admin can view the voting chat.");
        }
        requireOpenVotingRoom(seriesId);
        return boardChatMessageRepository.findRoomMessages(seriesId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public BoardChatMessageResponse sendMessage(Long seriesId, Long boardMemberId, String content) {
        User sender = requireUser(boardMemberId);
        if (!isBoard(roleName(sender))) {
            throw new AccessDeniedException("Only Editorial Board members can send voting chat messages.");
        }
        MangaSeries series = requireOpenVotingRoom(seriesId);
        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedContent.isEmpty()) {
            throw new RuntimeException("Voting chat message is required.");
        }
        if (normalizedContent.length() > MAX_MESSAGE_LENGTH) {
            throw new RuntimeException("Voting chat messages cannot exceed " + MAX_MESSAGE_LENGTH + " characters.");
        }

        BoardChatMessage saved = boardChatMessageRepository.save(
                new BoardChatMessage(null, series, sender, normalizedContent, null));
        return toResponse(saved);
    }

    private MangaSeries requireOpenVotingRoom(Long seriesId) {
        MangaSeries series = mangaSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new RuntimeException("Manga Series not found."));
        if (!"REVIEWING".equalsIgnoreCase(series.getStatus())) {
            throw new AccessDeniedException("The voting chat is available only before the Admin final decision.");
        }
        return series;
    }

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found."));
    }

    private String roleName(User user) {
        return user.getRole() != null && user.getRole().getRoleName() != null
                ? user.getRole().getRoleName().trim()
                : "";
    }

    private boolean isBoard(String role) {
        return "Editorial Board".equalsIgnoreCase(role);
    }

    private boolean isAdmin(String role) {
        return "Admin".equalsIgnoreCase(role);
    }

    private BoardChatMessageResponse toResponse(BoardChatMessage message) {
        User sender = message.getSender();
        String senderName = sender != null && sender.getFullName() != null && !sender.getFullName().isBlank()
                ? sender.getFullName().trim()
                : sender != null && sender.getUsername() != null && !sender.getUsername().isBlank()
                    ? sender.getUsername().trim()
                    : "Editorial Board Member";
        return new BoardChatMessageResponse(
                message.getId(),
                message.getMangaSeries() != null ? message.getMangaSeries().getId() : null,
                sender != null ? sender.getId() : null,
                senderName,
                message.getContent(),
                message.getCreatedAt());
    }
}
