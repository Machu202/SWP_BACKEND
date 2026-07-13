package com.mangastudio.backend;

import com.mangastudio.backend.entity.*;
import com.mangastudio.backend.repository.MangaSeriesRepository;
import com.mangastudio.backend.repository.TaskRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskWorkflowRegressionTests {
    private TaskRepository taskRepository;
    private UserRepository userRepository;
    private MangaSeriesRepository seriesRepository;
    private TaskServiceImpl service;
    private User mangaka;
    private User assistant;

    @BeforeEach
    void setUp() {
        taskRepository = mock(TaskRepository.class);
        userRepository = mock(UserRepository.class);
        seriesRepository = mock(MangaSeriesRepository.class);
        service = new TaskServiceImpl(taskRepository, userRepository, seriesRepository);
        mangaka = user(1L, "Mangaka");
        assistant = user(2L, "Assistant");
        when(userRepository.findById(1L)).thenReturn(Optional.of(mangaka));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assistant));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void assistantKanbanIsReadOnlyAndMangakaCanOnlyMoveForward() {
        Task task = task(7L, "TODO");
        when(taskRepository.findById(7L)).thenReturn(Optional.of(task));

        assertThrows(AccessDeniedException.class, () -> service.updateTaskStatus(7L, 2L, "DOING"));
        assertEquals("DOING", service.updateTaskStatus(7L, 1L, "DOING").getStatus());
        assertEquals("REVIEWING", service.updateTaskStatus(7L, 1L, "REVIEWING").getStatus());
        assertThrows(RuntimeException.class, () -> service.updateTaskStatus(7L, 1L, "APPROVED"));
        assertThrows(RuntimeException.class, () -> service.updateTaskStatus(7L, 1L, "DOING"));
        assertEquals("APPROVED", service.reviewTask(7L, 1L, true).getStatus());
    }

    @Test
    void referenceDownloadStartsAssistantTaskAndFirstSeriesTaskIsNumberOne() {
        Task task = task(11L, "TODO");
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));
        when(taskRepository.countSeriesTasksUpToId(10L, 11L)).thenReturn(1L);

        assertEquals("DOING", service.startTask(11L, 2L).getStatus());
        assertEquals(1L, service.getSeriesTaskNumber(task));
    }

    private User user(Long id, String roleName) {
        return User.builder().id(id).username("user" + id).passwordHash("x").isActive(true)
                .role(Role.builder().id(id).roleName(roleName).build()).build();
    }

    private Task task(Long id, String status) {
        MangaSeries series = MangaSeries.builder().id(10L).title("New series").mangaka(mangaka).status("DRAFT").build();
        Chapter chapter = Chapter.builder().id(20L).mangaSeries(series).chapterNumber(1).title("Chapter 1").publishStatus("DRAFT").build();
        Page page = Page.builder().id(30L).chapter(chapter).pageNumber(1).imageUrl("reference.png").build();
        Hitbox hitbox = Hitbox.builder().id(40L).page(page).createdBy(mangaka).xCoord(1D).yCoord(1D).width(10D).height(10D).build();
        return Task.builder().id(id).hitbox(hitbox).mangaka(mangaka).assistant(assistant)
                .status(status).description("Task").build();
    }
}
