package com.mangastudio.backend;

import com.mangastudio.backend.dto.response.MangaSeriesResponse;
import com.mangastudio.backend.entity.Chapter;
import com.mangastudio.backend.entity.MangaSeries;
import com.mangastudio.backend.entity.Role;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.BoardVoteRepository;
import com.mangastudio.backend.repository.ChapterRepository;
import com.mangastudio.backend.repository.MangaSeriesRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.impl.MangaSeriesServiceImpl;
import com.mangastudio.backend.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class TantouEditorialBoardHandoffTests {

    private MangaSeriesRepository seriesRepository;
    private UserRepository userRepository;
    private BoardVoteRepository voteRepository;
    private ChapterRepository chapterRepository;
    private NotificationService notificationService;
    private MangaSeriesServiceImpl service;

    @BeforeEach
    void setUp() {
        seriesRepository = mock(MangaSeriesRepository.class);
        userRepository = mock(UserRepository.class);
        voteRepository = mock(BoardVoteRepository.class);
        chapterRepository = mock(ChapterRepository.class);
        notificationService = mock(NotificationService.class);
        service = new MangaSeriesServiceImpl(
                seriesRepository, userRepository, voteRepository, chapterRepository, notificationService,
                mock(com.mangastudio.backend.repository.BoardVoteHistoryRepository.class),
                mock(com.mangastudio.backend.repository.BoardChatMessageRepository.class),
                mock(com.mangastudio.backend.repository.DeadlineEventRepository.class),
                mock(com.mangastudio.backend.repository.PublishingScheduleRepository.class),
                mock(com.mangastudio.backend.repository.TelemetryAnalyticsRepository.class));
    }

    @Test
    void assignedTantouCanSubmitSeriesWhenEveryChapterIsApproved() {
        User mangaka = user(2L, "Mangaka");
        User tantou = user(4L, "Tantou Editor");
        MangaSeries series = series(20L, mangaka, tantou, "DRAFT");
        when(seriesRepository.findById(20L)).thenReturn(Optional.of(series));
        when(userRepository.findById(4L)).thenReturn(Optional.of(tantou));
        when(chapterRepository.findByMangaSeriesIdOrderByChapterNumberAsc(20L))
                .thenReturn(List.of(chapter(1L, series, 1, "APPROVED"), chapter(2L, series, 2, "APPROVED")));
        when(seriesRepository.save(series)).thenReturn(series);

        MangaSeriesResponse response = service.submitToEditorialBoard(20L, 4L);

        assertEquals("REVIEWING", response.getStatus());
        assertEquals("REVIEWING", series.getStatus());
        verify(voteRepository).deleteByMangaSeriesId(20L);
        verify(seriesRepository).save(series);
    }

    @Test
    void unfinishedChapterBlocksEditorialBoardSubmission() {
        User mangaka = user(2L, "Mangaka");
        User tantou = user(4L, "Tantou Editor");
        MangaSeries series = series(21L, mangaka, tantou, "DRAFT");
        when(seriesRepository.findById(21L)).thenReturn(Optional.of(series));
        when(userRepository.findById(4L)).thenReturn(Optional.of(tantou));
        when(chapterRepository.findByMangaSeriesIdOrderByChapterNumberAsc(21L))
                .thenReturn(List.of(chapter(3L, series, 1, "APPROVED"), chapter(4L, series, 2, "REVISION")));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.submitToEditorialBoard(21L, 4L));

        assertTrue(error.getMessage().contains("Every chapter must be APPROVED"));
        verify(voteRepository, never()).deleteByMangaSeriesId(any());
        verify(seriesRepository, never()).save(any());
    }

    @Test
    void mangakaCannotSubmitSeriesWithoutAssignedTantou() {
        User mangaka = user(2L, "Mangaka");
        MangaSeries series = series(24L, mangaka, null, "DRAFT");
        when(seriesRepository.findById(24L)).thenReturn(Optional.of(series));
        when(userRepository.findById(2L)).thenReturn(Optional.of(mangaka));

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.updateSeriesStatus(24L, 2L, "REVIEWING"));

        assertTrue(error.getMessage().contains("Assign a Tantou Editor"));
        verify(seriesRepository, never()).save(any());
    }

    @Test
    void unassignedTantouCannotSubmitSeries() {
        User mangaka = user(2L, "Mangaka");
        User assigned = user(4L, "Tantou Editor");
        User other = user(8L, "Tantou Editor");
        MangaSeries series = series(22L, mangaka, assigned, "DRAFT");
        when(seriesRepository.findById(22L)).thenReturn(Optional.of(series));
        when(userRepository.findById(8L)).thenReturn(Optional.of(other));

        assertThrows(AccessDeniedException.class, () -> service.submitToEditorialBoard(22L, 8L));
        verify(chapterRepository, never()).findByMangaSeriesIdOrderByChapterNumberAsc(any());
        verify(seriesRepository, never()).save(any());
    }

    @Test
    void mangakaCannotBypassTantouApprovalUsingGenericStatusEndpoint() {
        User mangaka = user(2L, "Mangaka");
        User tantou = user(4L, "Tantou Editor");
        MangaSeries series = series(23L, mangaka, tantou, "DRAFT");
        when(seriesRepository.findById(23L)).thenReturn(Optional.of(series));
        when(userRepository.findById(2L)).thenReturn(Optional.of(mangaka));
        when(chapterRepository.findByMangaSeriesIdOrderByChapterNumberAsc(23L))
                .thenReturn(List.of(chapter(5L, series, 1, "REVIEWING")));

        assertThrows(RuntimeException.class, () -> service.updateSeriesStatus(23L, 2L, "REVIEWING"));
        verify(seriesRepository, never()).save(any());
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

    private MangaSeries series(Long id, User mangaka, User tantou, String status) {
        return MangaSeries.builder()
                .id(id)
                .title("Series " + id)
                .mangaka(mangaka)
                .tantou(tantou)
                .status(status)
                .build();
    }

    private Chapter chapter(Long id, MangaSeries series, int chapterNumber, String status) {
        return Chapter.builder()
                .id(id)
                .mangaSeries(series)
                .chapterNumber(chapterNumber)
                .title("Chapter " + chapterNumber)
                .publishStatus(status)
                .build();
    }
}
