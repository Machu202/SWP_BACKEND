package com.mangastudio.backend;

import com.mangastudio.backend.entity.Chapter;
import com.mangastudio.backend.entity.Hitbox;
import com.mangastudio.backend.entity.MangaSeries;
import com.mangastudio.backend.entity.Page;
import com.mangastudio.backend.entity.Role;
import com.mangastudio.backend.entity.TantouFeedback;
import com.mangastudio.backend.entity.Task;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.PageRepository;
import com.mangastudio.backend.repository.TantouFeedbackRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.NotificationService;
import com.mangastudio.backend.service.WorkspaceService;
import com.mangastudio.backend.service.TaskService;
import com.mangastudio.backend.service.impl.TantouFeedbackServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TantouFeedbackAssistantTaskTests {

    @Test
    void owningMangakaCreatesTaskFromExactTantouAreaAndAttributedDescription() {
        Fixture fixture = new Fixture();
        when(fixture.feedbacks.findById(40L)).thenReturn(Optional.of(fixture.feedback));
        when(fixture.users.findById(1L)).thenReturn(Optional.of(fixture.mangaka));
        Hitbox clonedArea = Hitbox.builder().id(50L).build();
        when(fixture.workspace.createHitbox(30L, 1L, 10D, 20D, 30D, 40D)).thenReturn(clonedArea);
        when(fixture.workspace.assignTaskToHitbox(any(), any(), any())).thenAnswer(invocation -> {
            Task request = invocation.getArgument(2);
            return Task.builder().id(60L).hitbox(clonedArea).description(request.getDescription()).build();
        });
        when(fixture.tasks.assignAssistantToTask(60L, 1L, 3L)).thenReturn(
                Task.builder().id(60L).hitbox(clonedArea).description("BY TANTOU EDITOR\nAdjust this panel").build());

        Task task = fixture.service.createAssistantTask(40L, 1L, 3L);

        assertEquals("BY TANTOU EDITOR\nAdjust this panel", task.getDescription());
        verify(fixture.workspace).createHitbox(30L, 1L, 10D, 20D, 30D, 40D);
        verify(fixture.workspace).assignTaskToHitbox(50L, 1L, any(Task.class));
        verify(fixture.tasks).assignAssistantToTask(60L, 1L, 3L);
    }

    @Test
    void nonOwningMangakaCannotConvertTantouFeedback() {
        Fixture fixture = new Fixture();
        User otherMangaka = User.builder().id(9L).role(Role.builder().roleName("Mangaka").build()).build();
        when(fixture.feedbacks.findById(40L)).thenReturn(Optional.of(fixture.feedback));
        when(fixture.users.findById(9L)).thenReturn(Optional.of(otherMangaka));

        assertThrows(AccessDeniedException.class, () -> fixture.service.createAssistantTask(40L, 9L, 3L));
        verify(fixture.workspace, never()).createHitbox(any(), any(), any(), any(), any(), any());
    }

    @Test
    void mangakaCommentCannotBeConvertedAsIfItWereTantouFeedback() {
        Fixture fixture = new Fixture();
        fixture.feedback.setEditor(fixture.mangaka);
        when(fixture.feedbacks.findById(40L)).thenReturn(Optional.of(fixture.feedback));
        when(fixture.users.findById(1L)).thenReturn(Optional.of(fixture.mangaka));

        assertThrows(AccessDeniedException.class, () -> fixture.service.createAssistantTask(40L, 1L, 3L));
        verify(fixture.workspace, never()).createHitbox(any(), any(), any(), any(), any(), any());
    }

    private static class Fixture {
        private final TantouFeedbackRepository feedbacks = mock(TantouFeedbackRepository.class);
        private final PageRepository pages = mock(PageRepository.class);
        private final UserRepository users = mock(UserRepository.class);
        private final WorkspaceService workspace = mock(WorkspaceService.class);
        private final TaskService tasks = mock(TaskService.class);
        private final User mangaka = User.builder()
                .id(1L)
                .role(Role.builder().roleName("Mangaka").build())
                .build();
        private final User tantou = User.builder()
                .id(2L)
                .role(Role.builder().roleName("Tantou Editor").build())
                .build();
        private final MangaSeries series = MangaSeries.builder().id(10L).mangaka(mangaka).tantou(tantou).build();
        private final Chapter chapter = Chapter.builder().id(20L).mangaSeries(series).build();
        private final Page page = Page.builder().id(30L).chapter(chapter).build();
        private final TantouFeedback feedback = TantouFeedback.builder()
                .id(40L)
                .page(page)
                .editor(tantou)
                .xCoord(10D)
                .yCoord(20D)
                .width(30D)
                .height(40D)
                .content("Adjust this panel")
                .build();
        private final TantouFeedbackServiceImpl service = new TantouFeedbackServiceImpl(
                feedbacks, pages, users, mock(NotificationService.class), workspace, tasks);
    }
}
