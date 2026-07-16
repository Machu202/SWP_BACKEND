package com.mangastudio.backend;

import com.mangastudio.backend.entity.*;
import com.mangastudio.backend.repository.*;
import com.mangastudio.backend.service.NotificationService;
import com.mangastudio.backend.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AssistantApprovalPagePromotionTests {
    private TaskServiceImpl service(TaskRepository tasks, UserRepository users, MangaSeriesRepository series,
                                    PageRepository pages, PageVersionRepository versions,
                                    HitboxRepository hitboxes, NotificationService notifications) {
        return new TaskServiceImpl(tasks, users, series, pages, versions, hitboxes, notifications);
    }

    @Test
    void approvingSubmissionUpdatesLivePageCreatesVersionKeepsReferenceAndArchivesHitboxes() {
        TaskRepository tasks = mock(TaskRepository.class); UserRepository users = mock(UserRepository.class);
        MangaSeriesRepository series = mock(MangaSeriesRepository.class); PageRepository pages = mock(PageRepository.class);
        PageVersionRepository versions = mock(PageVersionRepository.class); HitboxRepository hitboxes = mock(HitboxRepository.class);
        TaskServiceImpl service = service(tasks, users, series, pages, versions, hitboxes, mock(NotificationService.class));
        User mangaka = User.builder().id(1L).role(Role.builder().roleName("Mangaka").build()).build();
        MangaSeries manga = MangaSeries.builder().id(2L).mangaka(mangaka).build();
        Chapter chapter = Chapter.builder().id(3L).mangaSeries(manga).build();
        Page page = Page.builder().id(4L).chapter(chapter).imageUrl("old.png").build();
        Hitbox hitbox = Hitbox.builder().id(5L).page(page).build();
        Task task = Task.builder().id(6L).mangaka(mangaka).hitbox(hitbox).status("REVIEWING").submittedImageUrl("approved.png").build();
        PageVersion oldVersion = PageVersion.builder().id(10L).page(page).imageUrl("old.png").versionNumber(1).build();
        when(tasks.findById(6L)).thenReturn(Optional.of(task)); when(tasks.save(any())).thenAnswer(i -> i.getArgument(0));
        when(pages.save(any())).thenAnswer(i -> i.getArgument(0));
        when(versions.save(any())).thenAnswer(i -> i.getArgument(0));
        when(versions.findTopByPageIdOrderByVersionNumberDesc(4L)).thenReturn(Optional.of(oldVersion));
        when(versions.findTopByPageIdAndImageUrlOrderByVersionNumberDesc(4L, "old.png")).thenReturn(Optional.of(oldVersion));
        when(hitboxes.findByPageIdAndPageVersionIsNull(4L)).thenReturn(List.of(hitbox));

        Task result = service.reviewTask(6L, 1L, true);

        assertEquals("APPROVED", result.getStatus());
        assertEquals("old.png", result.getReferenceImageUrl());
        assertEquals("approved.png", page.getImageUrl());
        assertSame(oldVersion, hitbox.getPageVersion());
        verify(hitboxes).saveAll(List.of(hitbox));
        verify(versions).save(argThat(v -> v.getVersionNumber() == 2 && "approved.png".equals(v.getImageUrl())));
    }

    @Test
    void reviewingAndApprovedTasksCannotBeReassigned() {
        TaskRepository tasks = mock(TaskRepository.class); UserRepository users = mock(UserRepository.class);
        TaskServiceImpl service = service(tasks, users, mock(MangaSeriesRepository.class), mock(PageRepository.class),
                mock(PageVersionRepository.class), mock(HitboxRepository.class), mock(NotificationService.class));
        User mangaka = User.builder().id(1L).role(Role.builder().roleName("Mangaka").build()).build();
        Task task = Task.builder().id(6L).mangaka(mangaka).status("REVIEWING").build();
        when(tasks.findById(6L)).thenReturn(Optional.of(task));
        assertThrows(RuntimeException.class, () -> service.assignAssistantToTask(6L, 1L, 2L));
        verify(users, never()).findById(2L);
    }

    @Test
    void legacyPageWithoutVersionsArchivesOldImageThenCreatesVersionTwo() {
        TaskRepository tasks = mock(TaskRepository.class); UserRepository users = mock(UserRepository.class);
        MangaSeriesRepository series = mock(MangaSeriesRepository.class); PageRepository pages = mock(PageRepository.class);
        PageVersionRepository versions = mock(PageVersionRepository.class); HitboxRepository hitboxes = mock(HitboxRepository.class);
        TaskServiceImpl service = service(tasks, users, series, pages, versions, hitboxes, mock(NotificationService.class));
        User mangaka = User.builder().id(1L).role(Role.builder().roleName("Mangaka").build()).build();
        Page page = Page.builder().id(4L).imageUrl("legacy.png").build();
        Hitbox hitbox = Hitbox.builder().id(5L).page(page).build();
        Task task = Task.builder().id(6L).mangaka(mangaka).hitbox(hitbox)
                .status("REVIEWING").description("Task").submittedImageUrl("approved.png").build();
        when(tasks.findById(6L)).thenReturn(Optional.of(task)); when(tasks.save(any())).thenAnswer(i -> i.getArgument(0));
        when(pages.save(any())).thenAnswer(i -> i.getArgument(0));
        when(versions.save(any())).thenAnswer(i -> i.getArgument(0));
        when(versions.findTopByPageIdOrderByVersionNumberDesc(4L)).thenReturn(Optional.empty());
        when(versions.findTopByPageIdAndImageUrlOrderByVersionNumberDesc(4L, "legacy.png")).thenReturn(Optional.empty());
        when(hitboxes.findByPageIdAndPageVersionIsNull(4L)).thenReturn(List.of(hitbox));

        service.reviewTask(6L, 1L, true);

        verify(versions).save(argThat(v -> v.getVersionNumber() == 1 && "legacy.png".equals(v.getImageUrl())));
        verify(versions).save(argThat(v -> v.getVersionNumber() == 2 && "approved.png".equals(v.getImageUrl())));
        assertNotNull(hitbox.getPageVersion());
        assertEquals("legacy.png", hitbox.getPageVersion().getImageUrl());
    }

    @Test
    void approvedLegacyTaskCanBeExplicitlyResynchronizedWithoutCloningSameImage() {
        TaskRepository tasks = mock(TaskRepository.class); PageRepository pages = mock(PageRepository.class);
        PageVersionRepository versions = mock(PageVersionRepository.class); HitboxRepository hitboxes = mock(HitboxRepository.class);
        TaskServiceImpl service = service(tasks, mock(UserRepository.class), mock(MangaSeriesRepository.class), pages, versions,
                hitboxes, mock(NotificationService.class));
        User mangaka = User.builder().id(1L).role(Role.builder().roleName("Mangaka").build()).build();
        Page page = Page.builder().id(4L).imageUrl("old.png").build();
        Hitbox hitbox = Hitbox.builder().id(5L).page(page).build();
        Task task = Task.builder().id(6L).mangaka(mangaka).hitbox(hitbox)
                .status("APPROVED").description("Task").referenceImageUrl("old.png").submittedImageUrl("approved.png").build();
        PageVersion oldVersion = PageVersion.builder().id(10L).page(page).imageUrl("old.png").versionNumber(1).build();
        when(tasks.findById(6L)).thenReturn(Optional.of(task)); when(tasks.save(any())).thenAnswer(i -> i.getArgument(0));
        when(pages.save(any())).thenAnswer(i -> i.getArgument(0)); when(versions.save(any())).thenAnswer(i -> i.getArgument(0));
        when(versions.findTopByPageIdOrderByVersionNumberDesc(4L)).thenReturn(Optional.of(oldVersion));
        when(versions.findTopByPageIdAndImageUrlOrderByVersionNumberDesc(4L, "old.png")).thenReturn(Optional.of(oldVersion));
        when(hitboxes.findByPageIdAndPageVersionIsNull(4L)).thenReturn(List.of(hitbox));

        service.reviewTask(6L, 1L, true);
        assertEquals("approved.png", page.getImageUrl());
        verify(versions, times(1)).save(any(PageVersion.class));

        service.reviewTask(6L, 1L, true);
        verify(versions, times(1)).save(any(PageVersion.class));
    }
}
