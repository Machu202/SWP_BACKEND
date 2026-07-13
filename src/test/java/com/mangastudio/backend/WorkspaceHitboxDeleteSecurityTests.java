package com.mangastudio.backend;

import com.mangastudio.backend.entity.*;
import com.mangastudio.backend.repository.*;
import com.mangastudio.backend.service.impl.WorkspaceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class WorkspaceHitboxDeleteSecurityTests {

    private HitboxRepository hitboxRepository;
    private PageRepository pageRepository;
    private UserRepository userRepository;
    private TaskRepository taskRepository;
    private HitboxCommentRepository commentRepository;
    private WorkspaceServiceImpl service;
    private Hitbox hitbox;

    @BeforeEach
    void setUp() {
        hitboxRepository = mock(HitboxRepository.class);
        pageRepository = mock(PageRepository.class);
        userRepository = mock(UserRepository.class);
        taskRepository = mock(TaskRepository.class);
        commentRepository = mock(HitboxCommentRepository.class);
        service = new WorkspaceServiceImpl(hitboxRepository, pageRepository, userRepository, taskRepository, commentRepository);

        User owner = User.builder().id(2L).role(Role.builder().id(1L).roleName("Mangaka").build()).build();
        MangaSeries series = MangaSeries.builder().id(5L).mangaka(owner).title("Owned series").build();
        Chapter chapter = Chapter.builder().id(6L).mangaSeries(series).chapterNumber(1).build();
        Page page = Page.builder().id(7L).chapter(chapter).pageNumber(1).imageUrl("image").build();
        hitbox = Hitbox.builder().id(8L).page(page).createdBy(owner).xCoord(1d).yCoord(1d).width(10d).height(10d).build();
        when(hitboxRepository.findById(8L)).thenReturn(Optional.of(hitbox));
    }

    @Test
    void owningMangakaCanDeleteUnusedHitbox() {
        when(taskRepository.findByHitboxId(8L)).thenReturn(null);

        service.deleteHitbox(8L, 2L);

        verify(commentRepository).deleteByHitboxId(8L);
        verify(hitboxRepository).delete(hitbox);
    }

    @Test
    void anotherUserCannotDeleteHitbox() {
        assertThrows(AccessDeniedException.class, () -> service.deleteHitbox(8L, 3L));
        verify(hitboxRepository, never()).delete(any());
    }

    @Test
    void hitboxWithTaskCannotBeDeletedAccidentally() {
        when(taskRepository.findByHitboxId(8L)).thenReturn(Task.builder().id(9L).hitbox(hitbox).description("work").build());

        assertThrows(RuntimeException.class, () -> service.deleteHitbox(8L, 2L));
        verify(hitboxRepository, never()).delete(any());
    }
}
