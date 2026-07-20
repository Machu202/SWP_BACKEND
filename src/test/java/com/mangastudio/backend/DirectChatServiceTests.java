package com.mangastudio.backend;

import com.mangastudio.backend.dto.response.DirectChatMessageResponse;
import com.mangastudio.backend.entity.DirectChatMessage;
import com.mangastudio.backend.entity.Role;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.entity.MangaSeries;
import com.mangastudio.backend.repository.DirectChatMessageRepository;
import com.mangastudio.backend.repository.MangaSeriesRepository;
import com.mangastudio.backend.repository.TaskRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.impl.DirectChatServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DirectChatServiceTests {

    @Test
    void mangakaAndAssistantCanExchangePersistedMessages() {
        Fixture fixture = new Fixture();
        fixture.allow(fixture.mangaka, fixture.assistant);
        when(fixture.messages.save(any(DirectChatMessage.class))).thenAnswer(invocation -> {
            DirectChatMessage message = invocation.getArgument(0);
            message.setId(70L);
            return message;
        });

        DirectChatMessageResponse saved = fixture.service.sendMessage(1L, 2L, "  Please check page three  ");
        when(fixture.messages.findConversation(2L, 1L)).thenReturn(List.of(
                new DirectChatMessage(saved.getId(), fixture.mangaka, fixture.assistant, saved.getContent(), saved.getCreatedAt())));

        assertEquals("Please check page three", saved.getContent());
        assertEquals(1, fixture.service.getMessages(2L, 1L).size());
    }

    @Test
    void mangakaAndTantouCanExchangePersistedMessages() {
        Fixture fixture = new Fixture();
        fixture.allow(fixture.mangaka, fixture.tantou);
        when(fixture.messages.findConversation(1L, 3L)).thenReturn(List.of());

        assertEquals(0, fixture.service.getMessages(1L, 3L).size());
        verify(fixture.messages).findConversation(1L, 3L);
    }

    @Test
    void assistantAndTantouCannotOpenDirectChat() {
        Fixture fixture = new Fixture();
        fixture.allow(fixture.assistant, fixture.tantou);

        assertThrows(AccessDeniedException.class, () -> fixture.service.getMessages(2L, 3L));
        verify(fixture.messages, never()).findConversation(2L, 3L);
    }

    @Test
    void contactListUsesAssignedSeriesAndIncludesUnreadCount() {
        Fixture fixture = new Fixture();
        MangaSeries series = MangaSeries.builder()
                .id(10L).title("Ink Horizon").mangaka(fixture.mangaka).tantou(fixture.tantou).build();
        when(fixture.users.findById(1L)).thenReturn(Optional.of(fixture.mangaka));
        when(fixture.tasks.findByMangakaId(1L)).thenReturn(List.of());
        when(fixture.series.findByMangakaId(1L)).thenReturn(List.of(series));
        when(fixture.messages.countByReceiver_IdAndSender_IdAndReadAtIsNull(1L, 3L)).thenReturn(4L);

        var contacts = fixture.service.getContacts(1L);

        assertEquals(1, contacts.size());
        assertEquals("Taro Editor", contacts.get(0).getFullName());
        assertEquals(List.of("Ink Horizon"), contacts.get(0).getSeriesTitles());
        assertEquals(4L, contacts.get(0).getUnreadCount());
    }

    @Test
    void sameRolePairWithoutAWorkingRelationshipCannotChat() {
        Fixture fixture = new Fixture();
        when(fixture.users.findById(1L)).thenReturn(Optional.of(fixture.mangaka));
        when(fixture.users.findById(2L)).thenReturn(Optional.of(fixture.assistant));
        when(fixture.tasks.existsByMangaka_IdAndAssistant_Id(1L, 2L)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> fixture.service.getMessages(1L, 2L));
        verify(fixture.messages, never()).findConversation(1L, 2L);
    }

    private static class Fixture {
        private final DirectChatMessageRepository messages = mock(DirectChatMessageRepository.class);
        private final UserRepository users = mock(UserRepository.class);
        private final TaskRepository tasks = mock(TaskRepository.class);
        private final MangaSeriesRepository series = mock(MangaSeriesRepository.class);
        private final DirectChatServiceImpl service = new DirectChatServiceImpl(messages, users, tasks, series);
        private final User mangaka = user(1L, "Mangaka", "Mika Mangaka");
        private final User assistant = user(2L, "Assistant", "Aya Assistant");
        private final User tantou = user(3L, "Tantou Editor", "Taro Editor");

        private void allow(User first, User second) {
            when(users.findById(first.getId())).thenReturn(Optional.of(first));
            when(users.findById(second.getId())).thenReturn(Optional.of(second));
            String firstRole = first.getRole().getRoleName();
            String secondRole = second.getRole().getRoleName();
            if (("Mangaka".equals(firstRole) && "Assistant".equals(secondRole))
                    || ("Assistant".equals(firstRole) && "Mangaka".equals(secondRole))) {
                Long mangakaId = "Mangaka".equals(firstRole) ? first.getId() : second.getId();
                Long assistantId = "Assistant".equals(firstRole) ? first.getId() : second.getId();
                when(tasks.existsByMangaka_IdAndAssistant_Id(mangakaId, assistantId)).thenReturn(true);
            }
            if (("Mangaka".equals(firstRole) && "Tantou Editor".equals(secondRole))
                    || ("Tantou Editor".equals(firstRole) && "Mangaka".equals(secondRole))) {
                Long mangakaId = "Mangaka".equals(firstRole) ? first.getId() : second.getId();
                Long tantouId = "Tantou Editor".equals(firstRole) ? first.getId() : second.getId();
                when(series.existsByMangaka_IdAndTantou_Id(mangakaId, tantouId)).thenReturn(true);
            }
        }

        private static User user(Long id, String role, String name) {
            return User.builder()
                    .id(id)
                    .role(Role.builder().roleName(role).build())
                    .username(name.replace(" ", "").toLowerCase())
                    .fullName(name)
                    .isActive(true)
                    .build();
        }
    }
}
