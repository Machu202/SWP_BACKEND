package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.dto.response.DirectChatMessageResponse;
import com.mangastudio.backend.entity.DirectChatMessage;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.DirectChatMessageRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.DirectChatService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class DirectChatServiceImpl implements DirectChatService {

    private static final int MAX_MESSAGE_LENGTH = 2000;

    private final DirectChatMessageRepository directChatMessageRepository;
    private final UserRepository userRepository;

    public DirectChatServiceImpl(DirectChatMessageRepository directChatMessageRepository,
                                 UserRepository userRepository) {
        this.directChatMessageRepository = directChatMessageRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DirectChatMessageResponse> getMessages(Long currentUserId, Long otherUserId) {
        Participants participants = requireAllowedParticipants(currentUserId, otherUserId);
        return directChatMessageRepository.findConversation(participants.currentUser().getId(), participants.otherUser().getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public DirectChatMessageResponse sendMessage(Long senderId, Long recipientId, String content) {
        Participants participants = requireAllowedParticipants(senderId, recipientId);
        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedContent.isEmpty()) {
            throw new RuntimeException("Chat message is required.");
        }
        if (normalizedContent.length() > MAX_MESSAGE_LENGTH) {
            throw new RuntimeException("Chat messages cannot exceed " + MAX_MESSAGE_LENGTH + " characters.");
        }

        DirectChatMessage saved = directChatMessageRepository.save(
                new DirectChatMessage(null, participants.currentUser(), participants.otherUser(), normalizedContent, null));
        return toResponse(saved);
    }

    private Participants requireAllowedParticipants(Long currentUserId, Long otherUserId) {
        if (currentUserId == null || otherUserId == null || currentUserId.equals(otherUserId)) {
            throw new AccessDeniedException("A direct chat requires two different users.");
        }

        User currentUser = requireActiveUser(currentUserId);
        User otherUser = requireActiveUser(otherUserId);
        if (!isAllowedRolePair(roleName(currentUser), roleName(otherUser))) {
            throw new AccessDeniedException("Direct chat is allowed only between Mangaka and Assistant or Mangaka and Tantou Editor.");
        }
        return new Participants(currentUser, otherUser);
    }

    private User requireActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found."));
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new AccessDeniedException("Chat is unavailable for an inactive user.");
        }
        return user;
    }

    private String roleName(User user) {
        if (user.getRole() == null || user.getRole().getRoleName() == null) return "";
        return user.getRole().getRoleName().trim().replace('_', ' ').toUpperCase(Locale.ROOT);
    }

    private boolean isAllowedRolePair(String firstRole, String secondRole) {
        boolean mangakaAssistant = ("MANGAKA".equals(firstRole) && "ASSISTANT".equals(secondRole))
                || ("ASSISTANT".equals(firstRole) && "MANGAKA".equals(secondRole));
        boolean mangakaTantou = ("MANGAKA".equals(firstRole) && "TANTOU EDITOR".equals(secondRole))
                || ("TANTOU EDITOR".equals(firstRole) && "MANGAKA".equals(secondRole));
        return mangakaAssistant || mangakaTantou;
    }

    private DirectChatMessageResponse toResponse(DirectChatMessage message) {
        User sender = message.getSender();
        User recipient = message.getRecipient();
        return new DirectChatMessageResponse(
                message.getId(),
                sender != null ? sender.getId() : null,
                recipient != null ? recipient.getId() : null,
                displayName(sender),
                displayName(recipient),
                message.getContent(),
                message.getCreatedAt());
    }

    private String displayName(User user) {
        if (user == null) return "User";
        if (user.getFullName() != null && !user.getFullName().isBlank()) return user.getFullName().trim();
        if (user.getUsername() != null && !user.getUsername().isBlank()) return user.getUsername().trim();
        return "User #" + user.getId();
    }

    private record Participants(User currentUser, User otherUser) {
    }
}
