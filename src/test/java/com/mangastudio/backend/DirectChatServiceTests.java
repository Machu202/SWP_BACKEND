package com.mangastudio.backend;

import com.mangastudio.backend.dto.response.DirectChatMessageResponse;
import com.mangastudio.backend.entity.DirectChatMessage;
import com.mangastudio.backend.entity.Role;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.DirectChatMessageRepository;
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

    private static class Fixture {
        private final DirectChatMessageRepository messages = mock(DirectChatMessageRepository.class);
        private final UserRepository users = mock(UserRepository.class);
        private final DirectChatServiceImpl service = new DirectChatServiceImpl(messages, users);
        private final User mangaka = user(1L, "Mangaka", "Mika Mangaka");
        private final User assistant = user(2L, "Assistant", "Aya Assistant");
        private final User tantou = user(3L, "Tantou Editor", "Taro Editor");

        private void allow(User first, User second) {
            when(users.findById(first.getId())).thenReturn(Optional.of(first));
            when(users.findById(second.getId())).thenReturn(Optional.of(second));
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
