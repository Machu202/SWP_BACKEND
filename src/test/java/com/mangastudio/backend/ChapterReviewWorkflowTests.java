package com.mangastudio.backend;

import com.mangastudio.backend.dto.response.ChapterResponse;
import com.mangastudio.backend.entity.Chapter;
import com.mangastudio.backend.entity.MangaSeries;
import com.mangastudio.backend.entity.Role;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.ChapterRepository;
import com.mangastudio.backend.repository.MangaSeriesRepository;
import com.mangastudio.backend.repository.TaskRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.impl.ChapterServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChapterReviewWorkflowTests {

    private ChapterRepository chapterRepository;
    private MangaSeriesRepository mangaSeriesRepository;
    private UserRepository userRepository;
    private TaskRepository taskRepository;
    private ChapterServiceImpl service;

    @BeforeEach
    void setUp() {
        chapterRepository = mock(ChapterRepository.class);
        mangaSeriesRepository = mock(MangaSeriesRepository.class);
        userRepository = mock(UserRepository.class);
        taskRepository = mock(TaskRepository.class);
        service = new ChapterServiceImpl(chapterRepository, mangaSeriesRepository, userRepository, taskRepository);
    }

    @Test
    void legacyApprovedAssistantTaskAppearsInTantouQueue() {
        User mangaka = user(2L, "Mangaka");
        User tantou = user(4L, "Tantou Editor");
        MangaSeries series = series(6L, mangaka, tantou);
        Chapter chapter = chapter(6L, series, "DRAFT");

        when(userRepository.findById(4L)).thenReturn(Optional.of(tantou));
        when(chapterRepository.findByMangaSeries_Tantou_IdOrderByChapterNumberAsc(4L)).thenReturn(List.of(chapter));
        when(taskRepository.existsByHitbox_Page_Chapter_IdAndStatusIgnoreCase(6L, "APPROVED")).thenReturn(true);

        List<ChapterResponse> queue = service.getTantouReviewQueue(4L);

        assertEquals(1, queue.size());
        assertEquals("READY_FOR_TANTOU", queue.get(0).getPublishStatus());
        assertTrue(queue.get(0).getReviewReady());
        assertEquals(4L, queue.get(0).getTantouId());
    }

    @Test
    void mangakaApprovalCanSendDraftChapterToTantouReview() {
        User mangaka = user(2L, "Mangaka");
        User tantou = user(4L, "Tantou Editor");
        MangaSeries series = series(6L, mangaka, tantou);
        Chapter chapter = chapter(6L, series, "DRAFT");

        when(chapterRepository.findById(6L)).thenReturn(Optional.of(chapter));
        when(userRepository.findById(2L)).thenReturn(Optional.of(mangaka));
        when(taskRepository.countByHitbox_Page_Chapter_Id(6L)).thenReturn(1L);
        when(taskRepository.countByHitbox_Page_Chapter_IdAndStatusIgnoreCase(6L, "APPROVED")).thenReturn(1L);
        when(chapterRepository.save(chapter)).thenReturn(chapter);

        ChapterResponse updated = service.updateChapterStatus(6L, 2L, "REVIEWING");

        assertEquals("REVIEWING", updated.getPublishStatus());
        assertEquals("REVIEWING", chapter.getPublishStatus());
        verify(chapterRepository).save(chapter);
    }


    @Test
    void mangakaCannotSendChapterWithoutAssignedTantou() {
        User mangaka = user(2L, "Mangaka");
        MangaSeries series = series(7L, mangaka, null);
        Chapter chapter = chapter(7L, series, "DRAFT");

        when(chapterRepository.findById(7L)).thenReturn(Optional.of(chapter));
        when(userRepository.findById(2L)).thenReturn(Optional.of(mangaka));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.updateChapterStatus(7L, 2L, "REVIEWING"));
        assertTrue(error.getMessage().contains("Assign a Tantou Editor"));
        verify(chapterRepository, never()).save(any());
    }

    @Test
    void mangakaCannotSendChapterWhileAnyAssistantTaskIsNotApproved() {
        User mangaka = user(2L, "Mangaka");
        User tantou = user(4L, "Tantou Editor");
        MangaSeries series = series(8L, mangaka, tantou);
        Chapter chapter = chapter(8L, series, "DRAFT");

        when(chapterRepository.findById(8L)).thenReturn(Optional.of(chapter));
        when(userRepository.findById(2L)).thenReturn(Optional.of(mangaka));
        when(taskRepository.countByHitbox_Page_Chapter_Id(8L)).thenReturn(2L);
        when(taskRepository.countByHitbox_Page_Chapter_IdAndStatusIgnoreCase(8L, "APPROVED")).thenReturn(1L);

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.updateChapterStatus(8L, 2L, "REVIEWING"));
        assertTrue(error.getMessage().contains("All Assistant tasks"));
        verify(chapterRepository, never()).save(any());
    }

    @Test
    void assignedTantouCanApproveReviewingChapter() {
        User mangaka = user(2L, "Mangaka");
        User tantou = user(4L, "Tantou Editor");
        MangaSeries series = series(6L, mangaka, tantou);
        Chapter chapter = chapter(6L, series, "REVIEWING");

        when(chapterRepository.findById(6L)).thenReturn(Optional.of(chapter));
        when(userRepository.findById(4L)).thenReturn(Optional.of(tantou));
        when(chapterRepository.save(chapter)).thenReturn(chapter);

        ChapterResponse updated = service.updateChapterStatus(6L, 4L, "APPROVED");

        assertEquals("APPROVED", updated.getPublishStatus());
        verify(chapterRepository).save(chapter);
    }

    @Test
    void assignedTantouCanResolveLegacyDraftWithApprovedTask() {
        User mangaka = user(2L, "Mangaka");
        User tantou = user(4L, "Tantou Editor");
        MangaSeries series = series(6L, mangaka, tantou);
        Chapter chapter = chapter(6L, series, "DRAFT");

        when(chapterRepository.findById(6L)).thenReturn(Optional.of(chapter));
        when(userRepository.findById(4L)).thenReturn(Optional.of(tantou));
        when(taskRepository.existsByHitbox_Page_Chapter_IdAndStatusIgnoreCase(6L, "APPROVED")).thenReturn(true);
        when(chapterRepository.save(chapter)).thenReturn(chapter);

        ChapterResponse updated = service.updateChapterStatus(6L, 4L, "REVISION");

        assertEquals("REVISION", updated.getPublishStatus());
        verify(chapterRepository).save(chapter);
    }

    @Test
    void anotherTantouCannotDecideUnassignedChapter() {
        User mangaka = user(2L, "Mangaka");
        User assigned = user(4L, "Tantou Editor");
        User other = user(8L, "Tantou Editor");
        MangaSeries series = series(6L, mangaka, assigned);
        Chapter chapter = chapter(6L, series, "REVIEWING");

        when(chapterRepository.findById(6L)).thenReturn(Optional.of(chapter));
        when(userRepository.findById(8L)).thenReturn(Optional.of(other));

        assertThrows(AccessDeniedException.class,
                () -> service.updateChapterStatus(6L, 8L, "APPROVED"));
        verify(chapterRepository, never()).save(any());
    }

    @Test
    void nonTantouCannotReadTantouQueue() {
        User mangaka = user(2L, "Mangaka");
        when(userRepository.findById(2L)).thenReturn(Optional.of(mangaka));

        assertThrows(AccessDeniedException.class, () -> service.getTantouReviewQueue(2L));
        verify(chapterRepository, never()).findByMangaSeries_Tantou_IdOrderByChapterNumberAsc(anyLong());
    }

    private User user(Long id, String roleName) {
        return User.builder()
                .id(id)
                .username("user" + id)
                .fullName("User " + id)
                .role(Role.builder().id(id).roleName(roleName).build())
                .isActive(true)
                .build();
    }

    private MangaSeries series(Long id, User mangaka, User tantou) {
        return MangaSeries.builder()
                .id(id)
                .title("Series " + id)
                .mangaka(mangaka)
                .tantou(tantou)
                .status("APPROVED")
                .build();
    }

    private Chapter chapter(Long id, MangaSeries series, String status) {
        return Chapter.builder()
                .id(id)
                .mangaSeries(series)
                .chapterNumber(1)
                .title("Chapter 1")
                .publishStatus(status)
                .build();
    }
}
