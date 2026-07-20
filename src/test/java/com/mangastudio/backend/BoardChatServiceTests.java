package com.mangastudio.backend;

import com.mangastudio.backend.dto.response.BoardChatMessageResponse;
import com.mangastudio.backend.entity.BoardChatMessage;
import com.mangastudio.backend.entity.MangaSeries;
import com.mangastudio.backend.entity.Role;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.BoardChatMessageRepository;
import com.mangastudio.backend.repository.MangaSeriesRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.impl.BoardChatServiceImpl;
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

class BoardChatServiceTests {

    @Test
    void boardMemberSendsAndReadsSavedMessageDuringVoting() {
        Fixture fixture = new Fixture("REVIEWING");
        when(fixture.users.findById(2L)).thenReturn(Optional.of(fixture.boardMember));
        when(fixture.series.findById(10L)).thenReturn(Optional.of(fixture.mangaSeries));
        when(fixture.messages.save(any(BoardChatMessage.class))).thenAnswer(invocation -> {
            BoardChatMessage saved = invocation.getArgument(0);
            saved.setId(50L);
            return saved;
        });

        BoardChatMessageResponse saved = fixture.service.sendMessage(10L, 2L, "  Strengthen chapter two  ");
        when(fixture.messages.findRoomMessages(10L)).thenReturn(List.of(
                new BoardChatMessage(saved.getId(), fixture.mangaSeries, fixture.boardMember, saved.getContent(), saved.getCreatedAt())));

        List<BoardChatMessageResponse> room = fixture.service.getMessages(10L, 2L);

        assertEquals("Strengthen chapter two", saved.getContent());
        assertEquals("Board Member", saved.getSenderName());
        assertEquals(1, room.size());
        assertEquals(50L, room.get(0).getId());
    }

    @Test
    void adminCanReadButCannotSend() {
        Fixture fixture = new Fixture("REVIEWING");
        when(fixture.users.findById(3L)).thenReturn(Optional.of(fixture.admin));
        when(fixture.series.findById(10L)).thenReturn(Optional.of(fixture.mangaSeries));
        when(fixture.messages.findRoomMessages(10L)).thenReturn(List.of());

        assertEquals(0, fixture.service.getMessages(10L, 3L).size());
        assertThrows(AccessDeniedException.class,
                () -> fixture.service.sendMessage(10L, 3L, "Admin must not post"));
        verify(fixture.messages, never()).save(any(BoardChatMessage.class));
    }

    @Test
    void adminCannotViewRoomAfterFinalDecision() {
        Fixture fixture = new Fixture("APPROVED");
        when(fixture.users.findById(3L)).thenReturn(Optional.of(fixture.admin));
        when(fixture.series.findById(10L)).thenReturn(Optional.of(fixture.mangaSeries));

        assertThrows(AccessDeniedException.class, () -> fixture.service.getMessages(10L, 3L));
        verify(fixture.messages, never()).findRoomMessages(10L);
    }

    private static class Fixture {
        private final BoardChatMessageRepository messages = mock(BoardChatMessageRepository.class);
        private final MangaSeriesRepository series = mock(MangaSeriesRepository.class);
        private final UserRepository users = mock(UserRepository.class);
        private final User boardMember = User.builder()
                .id(2L)
                .role(Role.builder().roleName("Editorial Board").build())
                .fullName("Board Member")
                .build();
        private final User admin = User.builder()
                .id(3L)
                .role(Role.builder().roleName("Admin").build())
                .fullName("Admin User")
                .build();
        private final MangaSeries mangaSeries;
        private final BoardChatServiceImpl service = new BoardChatServiceImpl(messages, series, users);

        private Fixture(String status) {
            mangaSeries = MangaSeries.builder().id(10L).title("Doraemon").status(status).build();
        }
    }
}
