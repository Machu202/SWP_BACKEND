package com.mangastudio.backend;

import com.mangastudio.backend.entity.*;
import com.mangastudio.backend.repository.*;
import com.mangastudio.backend.service.NotificationService;
import com.mangastudio.backend.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TaskWorkflowRegressionTests {
    private TaskRepository taskRepository;
    private UserRepository userRepository;
    private MangaSeriesRepository seriesRepository;
    private PageRepository pageRepository;
    private PageVersionRepository pageVersionRepository;
    private HitboxRepository hitboxRepository;
    private NotificationService notificationService;
    private TaskServiceImpl service;
    private User mangaka;
    private User assistant;

    @BeforeEach
    void setUp() {
        taskRepository = mock(TaskRepository.class);
        userRepository = mock(UserRepository.class);
        seriesRepository = mock(MangaSeriesRepository.class);
        pageRepository = mock(PageRepository.class);
        pageVersionRepository = mock(PageVersionRepository.class);
        hitboxRepository = mock(HitboxRepository.class);
        notificationService = mock(NotificationService.class);
        service = new TaskServiceImpl(taskRepository, userRepository, seriesRepository, pageRepository,
                pageVersionRepository, hitboxRepository, notificationService);
        mangaka = user(1L, "Mangaka");
        assistant = user(2L, "Assistant");
        when(userRepository.findById(1L)).thenReturn(Optional.of(mangaka));
        when(userRepository.findById(2L)).thenReturn(Optional.of(assistant));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pageRepository.save(any(Page.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(pageVersionRepository.save(any(PageVersion.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void assistantKanbanIsReadOnlyAndMangakaCanOnlyMoveForward() {
        Task task = task(7L, "TODO", "submitted.png");
        when(taskRepository.findById(7L)).thenReturn(Optional.of(task));
        PageVersion oldVersion = PageVersion.builder().id(1L).page(task.getHitbox().getPage())
                .imageUrl("reference.png").versionNumber(1).build();
        when(pageVersionRepository.findTopByPageIdOrderByVersionNumberDesc(30L)).thenReturn(Optional.of(oldVersion));
        when(pageVersionRepository.findTopByPageIdAndImageUrlOrderByVersionNumberDesc(30L, "reference.png"))
                .thenReturn(Optional.of(oldVersion));
        when(hitboxRepository.findByPageIdAndPageVersionIsNull(30L)).thenReturn(List.of(task.getHitbox()));

        assertThrows(AccessDeniedException.class, () -> service.updateTaskStatus(7L, 2L, "DOING"));
        assertEquals("DOING", service.updateTaskStatus(7L, 1L, "DOING").getStatus());
        assertEquals("REVIEWING", service.updateTaskStatus(7L, 1L, "REVIEWING").getStatus());
        assertThrows(RuntimeException.class, () -> service.updateTaskStatus(7L, 1L, "APPROVED"));
        assertThrows(RuntimeException.class, () -> service.updateTaskStatus(7L, 1L, "DOING"));
        assertEquals("APPROVED", service.reviewTask(7L, 1L, true).getStatus());
    }

    @Test
    void mangakaCannotMoveDoingTaskToReviewingWithoutAssistantSubmission() {
        Task task = task(8L, "DOING", null);
        when(taskRepository.findById(8L)).thenReturn(Optional.of(task));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.updateTaskStatus(8L, 1L, "REVIEWING"));
        assertTrue(error.getMessage().contains("submitted image"));
        assertEquals("DOING", task.getStatus());
    }

    @Test
    void assignmentAndSubmissionCreateRoleSpecificNotifications() {
        Task task = task(9L, "TODO", null);
        task.setAssistant(null);
        when(taskRepository.findById(9L)).thenReturn(Optional.of(task));

        service.assignAssistantToTask(9L, 1L, 2L);
        verify(notificationService).createNotification(2L,
                "You got a new task from \"New series\" mangaka!", "/tasks?tab=assignments");

        service.submitTaskWork(9L, 2L, "finished.png");
        verify(notificationService).createNotification(1L,
                "Assistant \"user2\" has sent you his work. Go check it out!", "/assistant-review");
    }

    @Test
    void emptySubmittedImageIsRejected() {
        Task task = task(10L, "DOING", null);
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        assertThrows(RuntimeException.class, () -> service.submitTaskWork(10L, 2L, "  "));
        assertNull(task.getSubmittedImageUrl());
        assertEquals("DOING", task.getStatus());
    }

    @Test
    void referenceDownloadStartsAssistantTaskAndFirstSeriesTaskIsNumberOne() {
        Task task = task(11L, "TODO", "submitted.png");
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));
        when(taskRepository.countSeriesTasksUpToId(10L, 11L)).thenReturn(1L);

        assertEquals("DOING", service.startTask(11L, 2L).getStatus());
        assertEquals(1L, service.getSeriesTaskNumber(task));
    }

    private User user(Long id, String roleName) {
        return User.builder().id(id).username("user" + id).passwordHash("x").isActive(true)
                .role(Role.builder().id(id).roleName(roleName).build()).build();
    }

    private Task task(Long id, String status, String submittedImage) {
        MangaSeries series = MangaSeries.builder().id(10L).title("New series").mangaka(mangaka).status("DRAFT").build();
        Chapter chapter = Chapter.builder().id(20L).mangaSeries(series).chapterNumber(1).title("Chapter 1").publishStatus("DRAFT").build();
        Page page = Page.builder().id(30L).chapter(chapter).pageNumber(1).imageUrl("reference.png").build();
        Hitbox hitbox = Hitbox.builder().id(40L).page(page).createdBy(mangaka).xCoord(1D).yCoord(1D).width(10D).height(10D).build();
        return Task.builder().id(id).hitbox(hitbox).mangaka(mangaka).assistant(assistant)
                .status(status).description("Task").submittedImageUrl(submittedImage).build();
    }
}
