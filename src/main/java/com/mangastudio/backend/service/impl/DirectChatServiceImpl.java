package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.dto.response.DirectChatMessageResponse;
import com.mangastudio.backend.dto.response.DirectChatContactResponse;
import com.mangastudio.backend.entity.DirectChatMessage;
import com.mangastudio.backend.entity.MangaSeries;
import com.mangastudio.backend.entity.Task;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.DirectChatMessageRepository;
import com.mangastudio.backend.repository.MangaSeriesRepository;
import com.mangastudio.backend.repository.TaskRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.DirectChatService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Service
public class DirectChatServiceImpl implements DirectChatService {

    private static final int MAX_MESSAGE_LENGTH = 2000;

    private final DirectChatMessageRepository directChatMessageRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final MangaSeriesRepository mangaSeriesRepository;

    public DirectChatServiceImpl(DirectChatMessageRepository directChatMessageRepository,
                                 UserRepository userRepository,
                                 TaskRepository taskRepository,
                                 MangaSeriesRepository mangaSeriesRepository) {
        this.directChatMessageRepository = directChatMessageRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.mangaSeriesRepository = mangaSeriesRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DirectChatContactResponse> getContacts(Long currentUserId) {
        User currentUser = requireActiveUser(currentUserId);
        String currentRole = roleName(currentUser);
        Map<Long, ContactAccumulator> contacts = new LinkedHashMap<>();

        if ("MANGAKA".equals(currentRole)) {
            for (Task task : taskRepository.findByMangakaId(currentUserId)) {
                addContact(contacts, currentUserId, task.getAssistant(), taskSeriesTitle(task));
            }
            for (MangaSeries series : mangaSeriesRepository.findByMangakaId(currentUserId)) {
                addContact(contacts, currentUserId, series.getTantou(), seriesTitle(series));
            }
        } else if ("ASSISTANT".equals(currentRole)) {
            for (Task task : taskRepository.findByAssistantId(currentUserId)) {
                addContact(contacts, currentUserId, task.getMangaka(), taskSeriesTitle(task));
            }
        } else if ("TANTOU EDITOR".equals(currentRole)) {
            for (MangaSeries series : mangaSeriesRepository.findAssignedToTantou(currentUserId)) {
                addContact(contacts, currentUserId, series.getMangaka(), seriesTitle(series));
            }
        }

        List<DirectChatContactResponse> result = new ArrayList<>();
        for (ContactAccumulator contact : contacts.values()) {
            User user = contact.user;
            result.add(new DirectChatContactResponse(
                    user.getId(),
                    displayName(user),
                    user.getUsername(),
                    user.getRole() != null ? user.getRole().getRoleName() : "Studio member",
                    List.copyOf(contact.seriesTitles),
                    directChatMessageRepository.countByReceiver_IdAndSender_IdAndReadAtIsNull(currentUserId, user.getId())));
        }
        result.sort(Comparator.comparing(DirectChatContactResponse::getFullName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    @Override
    @Transactional
    public List<DirectChatMessageResponse> getMessages(Long currentUserId, Long otherUserId) {
        Participants participants = requireAllowedParticipants(currentUserId, otherUserId);
        directChatMessageRepository.markConversationRead(
                participants.currentUser().getId(),
                participants.otherUser().getId(),
                LocalDateTime.now());
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
        if (!hasWorkingRelationship(currentUser, otherUser)) {
            throw new AccessDeniedException("Direct chat is available only to users connected by an assigned task or manga series.");
        }
        return new Participants(currentUser, otherUser);
    }

    private boolean hasWorkingRelationship(User first, User second) {
        String firstRole = roleName(first);
        String secondRole = roleName(second);
        if ("MANGAKA".equals(firstRole) && "ASSISTANT".equals(secondRole)) {
            return taskRepository.existsByMangaka_IdAndAssistant_Id(first.getId(), second.getId());
        }
        if ("ASSISTANT".equals(firstRole) && "MANGAKA".equals(secondRole)) {
            return taskRepository.existsByMangaka_IdAndAssistant_Id(second.getId(), first.getId());
        }
        if ("MANGAKA".equals(firstRole) && "TANTOU EDITOR".equals(secondRole)) {
            return mangaSeriesRepository.existsByMangaka_IdAndTantou_Id(first.getId(), second.getId());
        }
        if ("TANTOU EDITOR".equals(firstRole) && "MANGAKA".equals(secondRole)) {
            return mangaSeriesRepository.existsByMangaka_IdAndTantou_Id(second.getId(), first.getId());
        }
        return false;
    }

    private void addContact(Map<Long, ContactAccumulator> contacts, Long currentUserId,
                            User contact, String seriesTitle) {
        if (contact == null || contact.getId() == null || contact.getId().equals(currentUserId)
                || Boolean.FALSE.equals(contact.getIsActive())) return;
        ContactAccumulator accumulator = contacts.computeIfAbsent(
                contact.getId(), ignored -> new ContactAccumulator(contact));
        if (seriesTitle != null && !seriesTitle.isBlank()) accumulator.seriesTitles.add(seriesTitle.trim());
    }

    private String taskSeriesTitle(Task task) {
        if (task == null || task.getHitbox() == null || task.getHitbox().getPage() == null
                || task.getHitbox().getPage().getChapter() == null) return "Manga Series";
        return seriesTitle(task.getHitbox().getPage().getChapter().getMangaSeries());
    }

    private String seriesTitle(MangaSeries series) {
        return series == null || series.getTitle() == null || series.getTitle().isBlank()
                ? "Manga Series"
                : series.getTitle().trim();
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
        User recipient = message.getReceiver();
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

    private static final class ContactAccumulator {
        private final User user;
        private final Set<String> seriesTitles = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        private ContactAccumulator(User user) {
            this.user = user;
        }
    }
}
