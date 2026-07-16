package com.mangastudio.backend;

import com.mangastudio.backend.entity.Task;
import com.mangastudio.backend.repository.MangaSeriesRepository;
import com.mangastudio.backend.repository.TaskRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.repository.PageRepository;
import com.mangastudio.backend.repository.PageVersionRepository;
import com.mangastudio.backend.repository.HitboxRepository;
import com.mangastudio.backend.service.NotificationService;
import com.mangastudio.backend.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class TantouTaskVisibilityTests {

    @Test
    void tantouKanbanLoadsOnlyTasksFromSeriesAssignedToThatTantou() {
        TaskRepository taskRepository = mock(TaskRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        MangaSeriesRepository mangaSeriesRepository = mock(MangaSeriesRepository.class);
        TaskServiceImpl service = new TaskServiceImpl(taskRepository, userRepository, mangaSeriesRepository, mock(PageRepository.class), mock(PageVersionRepository.class), mock(HitboxRepository.class), mock(NotificationService.class));

        Task assignedTask = Task.builder().id(11L).status("APPROVED").build();
        when(taskRepository.findByAssignedTantouId(4L)).thenReturn(List.of(assignedTask));

        List<Task> result = service.getTasksByTantou(4L);

        assertEquals(1, result.size());
        assertEquals(11L, result.get(0).getId());
        verify(taskRepository).findByAssignedTantouId(4L);
        verify(taskRepository, never()).findByAssistantId(anyLong());
        verify(taskRepository, never()).findByMangakaId(anyLong());
    }
}
