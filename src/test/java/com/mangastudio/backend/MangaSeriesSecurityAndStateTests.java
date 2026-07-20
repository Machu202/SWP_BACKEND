package com.mangastudio.backend;

import com.mangastudio.backend.dto.request.MangaSeriesCreateRequest;
import com.mangastudio.backend.dto.request.MangaSeriesUpdateRequest;
import com.mangastudio.backend.entity.MangaSeries;
import com.mangastudio.backend.entity.Role;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.BoardVoteRepository;
import com.mangastudio.backend.repository.ChapterRepository;
import com.mangastudio.backend.repository.MangaSeriesRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.repository.BoardVoteHistoryRepository;
import com.mangastudio.backend.repository.BoardChatMessageRepository;
import com.mangastudio.backend.repository.DeadlineEventRepository;
import com.mangastudio.backend.repository.PublishingScheduleRepository;
import com.mangastudio.backend.repository.TelemetryAnalyticsRepository;
import com.mangastudio.backend.service.impl.MangaSeriesServiceImpl;
import com.mangastudio.backend.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class MangaSeriesSecurityAndStateTests {

    private MangaSeriesRepository mangaSeriesRepository;
    private UserRepository userRepository;
    private BoardVoteRepository boardVoteRepository;
    private ChapterRepository chapterRepository;
    private NotificationService notificationService;
    private BoardVoteHistoryRepository boardVoteHistoryRepository;
    private BoardChatMessageRepository boardChatMessageRepository;
    private DeadlineEventRepository deadlineEventRepository;
    private PublishingScheduleRepository publishingScheduleRepository;
    private TelemetryAnalyticsRepository telemetryAnalyticsRepository;
    private com.mangastudio.backend.service.TelemetryBufferService telemetryBufferService;
    private MangaSeriesServiceImpl service;

    @BeforeEach
    void setUp() {
        mangaSeriesRepository = mock(MangaSeriesRepository.class);
        userRepository = mock(UserRepository.class);
        boardVoteRepository = mock(BoardVoteRepository.class);
        chapterRepository = mock(ChapterRepository.class);
        notificationService = mock(NotificationService.class);
        boardVoteHistoryRepository = mock(BoardVoteHistoryRepository.class);
        boardChatMessageRepository = mock(BoardChatMessageRepository.class);
        deadlineEventRepository = mock(DeadlineEventRepository.class);
        publishingScheduleRepository = mock(PublishingScheduleRepository.class);
        telemetryAnalyticsRepository = mock(TelemetryAnalyticsRepository.class);
        telemetryBufferService = mock(com.mangastudio.backend.service.TelemetryBufferService.class);
        service = new MangaSeriesServiceImpl(
                mangaSeriesRepository, userRepository, boardVoteRepository, chapterRepository, notificationService,
                boardVoteHistoryRepository, boardChatMessageRepository, deadlineEventRepository,
                publishingScheduleRepository, telemetryAnalyticsRepository, telemetryBufferService);
    }

    @Test
    void assistantCannotCreateSeries() {
        User assistant = user(3L, "Assistant");
        when(userRepository.findById(3L)).thenReturn(Optional.of(assistant));

        MangaSeriesCreateRequest request = new MangaSeriesCreateRequest();
        request.setTitle("Forbidden series");

        assertThrows(AccessDeniedException.class, () -> service.createSeries(3L, request));
        verify(mangaSeriesRepository, never()).save(any());
    }

    @Test
    void nonOwnerCannotEditOrDeleteSeries() {
        User owner = user(2L, "Mangaka");
        MangaSeries series = series(10L, owner, "DRAFT");
        when(mangaSeriesRepository.findById(10L)).thenReturn(Optional.of(series));

        assertThrows(AccessDeniedException.class,
                () -> service.updateSeriesMetadata(10L, 5L, new MangaSeriesUpdateRequest()));
        assertThrows(AccessDeniedException.class, () -> service.deleteSeries(10L, 5L));

        verify(mangaSeriesRepository, never()).delete(any());
    }

    @Test
    void assistantCannotSubmitMangakaSeriesForReview() {
        User owner = user(2L, "Mangaka");
        User assistant = user(3L, "Assistant");
        MangaSeries series = series(11L, owner, "DRAFT");
        when(mangaSeriesRepository.findById(11L)).thenReturn(Optional.of(series));
        when(userRepository.findById(3L)).thenReturn(Optional.of(assistant));

        assertThrows(AccessDeniedException.class,
                () -> service.updateSeriesStatus(11L, 3L, "REVIEWING"));
        verify(mangaSeriesRepository, never()).save(any());
    }


    @Test
    void ownerMangakaCanAssignValidTantouBeforeChapterReview() {
        User owner = user(2L, "Mangaka");
        User tantou = user(4L, "Tantou Editor");
        MangaSeries series = series(20L, owner, "DRAFT");
        when(mangaSeriesRepository.findById(20L)).thenReturn(Optional.of(series));
        when(userRepository.findById(2L)).thenReturn(Optional.of(owner));
        when(userRepository.findById(4L)).thenReturn(Optional.of(tantou));
        when(mangaSeriesRepository.save(series)).thenReturn(series);

        var response = service.assignTantou(20L, 2L, 4L);

        assertEquals(4L, response.getTantouId());
        assertEquals(4L, series.getTantou().getId());
        verify(mangaSeriesRepository).save(series);
        verify(notificationService).createNotification(
                4L,
                "\"Series 20\" Mangaka has assigned you to review!",
                "/series");
    }

    @Test
    void tantouCannotBeAssignedToASecondSeries() {
        User owner = user(2L, "Mangaka");
        User tantou = user(4L, "Tantou Editor");
        MangaSeries series = series(22L, owner, "DRAFT");
        when(mangaSeriesRepository.findById(22L)).thenReturn(Optional.of(series));
        when(userRepository.findById(2L)).thenReturn(Optional.of(owner));
        when(userRepository.findById(4L)).thenReturn(Optional.of(tantou));
        when(mangaSeriesRepository.existsByTantou_IdAndIdNot(4L, 22L)).thenReturn(true);

        RuntimeException error = assertThrows(RuntimeException.class,
                () -> service.assignTantou(22L, 2L, 4L));

        assertEquals(
                "Tantou Editor is already assigned to another manga series. Each Tantou Editor can only be assigned to one manga series.",
                error.getMessage());
        verify(mangaSeriesRepository, never()).save(any());
        verify(notificationService, never()).createNotification(anyLong(), anyString(), anyString());
    }

    @Test
    void nonOwnerCannotAssignTantouAndAssignedUserMustBeTantou() {
        User owner = user(2L, "Mangaka");
        User assistant = user(3L, "Assistant");
        MangaSeries series = series(21L, owner, "DRAFT");
        when(mangaSeriesRepository.findById(21L)).thenReturn(Optional.of(series));
        when(userRepository.findById(3L)).thenReturn(Optional.of(assistant));

        assertThrows(AccessDeniedException.class, () -> service.assignTantou(21L, 3L, 4L));

        when(userRepository.findById(2L)).thenReturn(Optional.of(owner));
        when(userRepository.findById(3L)).thenReturn(Optional.of(assistant));
        assertThrows(RuntimeException.class, () -> service.assignTantou(21L, 2L, 3L));
        verify(mangaSeriesRepository, never()).save(any());
    }

    @Test
    void genericStatusEndpointCannotApproveReviewingSeries() {
        User owner = user(2L, "Mangaka");
        MangaSeries series = series(12L, owner, "REVIEWING");
        when(mangaSeriesRepository.findById(12L)).thenReturn(Optional.of(series));
        when(userRepository.findById(2L)).thenReturn(Optional.of(owner));

        assertThrows(RuntimeException.class,
                () -> service.updateSeriesStatus(12L, 2L, "APPROVED"));
        verify(mangaSeriesRepository, never()).save(any());
    }

    @Test
    void adminCannotDecideAlreadyApprovedSeries() {
        MangaSeries series = series(6L, user(2L, "Mangaka"), "APPROVED");
        when(mangaSeriesRepository.findById(6L)).thenReturn(Optional.of(series));

        assertThrows(RuntimeException.class,
                () -> service.handleAdminDecision(6L, true, 4L));
        verify(mangaSeriesRepository, never()).save(any());
    }

    @Test
    void approvalRequiresBoardVoteAndValidTantouRole() {
        MangaSeries series = series(13L, user(2L, "Mangaka"), "REVIEWING");
        when(mangaSeriesRepository.findById(13L)).thenReturn(Optional.of(series));
        when(boardVoteRepository.countByMangaSeriesIdAndIsApproved(13L, true)).thenReturn(0L);

        assertThrows(RuntimeException.class,
                () -> service.handleAdminDecision(13L, true, 4L));

        when(boardVoteRepository.countByMangaSeriesIdAndIsApproved(13L, true)).thenReturn(1L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user(7L, "Admin")));

        assertThrows(RuntimeException.class,
                () -> service.handleAdminDecision(13L, true, 7L));
        verify(mangaSeriesRepository, never()).save(any());
    }

    @Test
    void validAdminDecisionStillWorks() {
        MangaSeries series = series(14L, user(2L, "Mangaka"), "REVIEWING");
        User tantou = user(4L, "Tantou Editor");
        when(mangaSeriesRepository.findById(14L)).thenReturn(Optional.of(series));
        when(boardVoteRepository.countByMangaSeriesIdAndIsApproved(14L, true)).thenReturn(1L);
        when(userRepository.findById(4L)).thenReturn(Optional.of(tantou));
        when(mangaSeriesRepository.save(series)).thenReturn(series);

        MangaSeries result = service.handleAdminDecision(14L, true, 4L);

        assertEquals("APPROVED", result.getStatus());
        assertEquals(4L, result.getTantou().getId());
        verify(mangaSeriesRepository).save(series);
    }

    @Test
    void approvedEmptySeriesCannotBePublished() {
        User owner = user(2L, "Mangaka");
        MangaSeries series = series(16L, owner, "APPROVED");
        when(mangaSeriesRepository.findById(16L)).thenReturn(Optional.of(series));
        when(userRepository.findById(2L)).thenReturn(Optional.of(owner));
        when(chapterRepository.findByMangaSeriesIdOrderByChapterNumberAsc(16L)).thenReturn(java.util.List.of());

        assertThrows(RuntimeException.class, () -> service.updateSeriesStatus(16L, 2L, "ONGOING"));

        verify(mangaSeriesRepository, never()).save(any());
        verify(telemetryBufferService, never()).initializeSeries(anyLong());
    }

    @Test
    void approvedSeriesPublishesFirstApprovedChapterAndInitializesTelemetry() {
        User owner = user(2L, "Mangaka");
        MangaSeries series = series(17L, owner, "APPROVED");
        var chapter = com.mangastudio.backend.entity.Chapter.builder()
                .id(170L).mangaSeries(series).chapterNumber(1).publishStatus("APPROVED").build();
        when(mangaSeriesRepository.findById(17L)).thenReturn(Optional.of(series));
        when(userRepository.findById(2L)).thenReturn(Optional.of(owner));
        when(chapterRepository.findByMangaSeriesIdOrderByChapterNumberAsc(17L))
                .thenReturn(java.util.List.of(chapter));
        when(mangaSeriesRepository.save(series)).thenReturn(series);

        var response = service.updateSeriesStatus(17L, 2L, "ONGOING");

        assertEquals("ONGOING", response.getStatus());
        assertEquals("PUBLISHED", chapter.getPublishStatus());
        verify(chapterRepository).save(chapter);
        verify(telemetryBufferService).initializeSeries(17L);
        verify(publishingScheduleRepository)
                .deleteByMangaSeriesIdAndFrequencyIgnoreCase(17L, "SERIES_LAUNCH");
    }

    @Test
    void approvedOwnerCanScheduleFirstChapterForFutureLaunch() {
        User owner = user(2L, "Mangaka");
        MangaSeries series = series(18L, owner, "APPROVED");
        var chapter = com.mangastudio.backend.entity.Chapter.builder()
                .id(180L).mangaSeries(series).chapterNumber(1).publishStatus("APPROVED").build();
        when(mangaSeriesRepository.findById(18L)).thenReturn(Optional.of(series));
        when(userRepository.findById(2L)).thenReturn(Optional.of(owner));
        when(chapterRepository.findByMangaSeriesIdOrderByChapterNumberAsc(18L))
                .thenReturn(java.util.List.of(chapter));
        when(publishingScheduleRepository.save(any(com.mangastudio.backend.entity.PublishingSchedule.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        LocalDateTime publishAt = LocalDateTime.now().plusDays(1);

        var schedule = service.schedulePublication(18L, 2L, publishAt);

        assertEquals("SCHEDULED", chapter.getPublishStatus());
        assertEquals("SERIES_LAUNCH", schedule.getFrequency());
        assertEquals(publishAt, schedule.getPublishDate());
        verify(publishingScheduleRepository)
                .deleteByMangaSeriesIdAndFrequencyIgnoreCase(18L, "SERIES_LAUNCH");
        verify(telemetryBufferService, never()).initializeSeries(anyLong());
    }

    @Test
    void adminCannotAssignAnOccupiedTantouDuringFinalApproval() {
        MangaSeries series = series(15L, user(2L, "Mangaka"), "REVIEWING");
        User tantou = user(4L, "Tantou Editor");
        when(mangaSeriesRepository.findById(15L)).thenReturn(Optional.of(series));
        when(boardVoteRepository.countByMangaSeriesIdAndIsApproved(15L, true)).thenReturn(1L);
        when(userRepository.findById(4L)).thenReturn(Optional.of(tantou));
        when(mangaSeriesRepository.existsByTantou_IdAndIdNot(4L, 15L)).thenReturn(true);

        assertThrows(RuntimeException.class,
                () -> service.handleAdminDecision(15L, true, 4L));

        verify(mangaSeriesRepository, never()).save(any());
    }


    @Test
    void rejectedOwnerCanRevertSeriesToDraftWithoutDeletingVotesEarly() {
        User owner = user(2L, "Mangaka");
        MangaSeries series = series(30L, owner, "REJECTED");
        when(mangaSeriesRepository.findById(30L)).thenReturn(Optional.of(series));
        when(userRepository.findById(2L)).thenReturn(Optional.of(owner));
        when(mangaSeriesRepository.save(series)).thenReturn(series);

        var response = service.updateSeriesStatus(30L, 2L, "DRAFT");

        assertEquals("DRAFT", response.getStatus());
        assertEquals("DRAFT", series.getStatus());
        verify(boardVoteRepository, never()).deleteByMangaSeriesId(30L);
        verify(mangaSeriesRepository).save(series);
    }

    @Test
    void rejectedOwnerCanDeleteSeriesAndEverySeriesOwnedRecord() {
        User owner = user(2L, "Mangaka");
        MangaSeries series = series(40L, owner, "REJECTED");
        var chapter = com.mangastudio.backend.entity.Chapter.builder()
                .id(401L).mangaSeries(series).chapterNumber(1).publishStatus("APPROVED").build();
        when(mangaSeriesRepository.findById(40L)).thenReturn(Optional.of(series));
        when(chapterRepository.findByMangaSeriesIdOrderByChapterNumberAsc(40L)).thenReturn(java.util.List.of(chapter));

        service.deleteSeries(40L, 2L);

        verify(boardChatMessageRepository).deleteByMangaSeriesId(40L);
        verify(boardVoteHistoryRepository).deleteByMangaSeriesId(40L);
        verify(boardVoteRepository).deleteByMangaSeriesId(40L);
        verify(deadlineEventRepository).deleteByMangaSeriesId(40L);
        verify(publishingScheduleRepository).deleteByMangaSeriesId(40L);
        verify(telemetryAnalyticsRepository).deleteByMangaSeriesId(40L);
        verify(chapterRepository).deleteAll(java.util.List.of(chapter));
        verify(chapterRepository).flush();
        verify(mangaSeriesRepository).delete(series);
    }

    @Test
    void resubmittingDraftStartsFreshBoardVoteCycle() {
        User owner = user(2L, "Mangaka");
        User tantou = user(4L, "Tantou Editor");
        MangaSeries series = series(31L, owner, "DRAFT");
        series.setTantou(tantou);
        when(mangaSeriesRepository.findById(31L)).thenReturn(Optional.of(series));
        when(userRepository.findById(2L)).thenReturn(Optional.of(owner));
        when(chapterRepository.findByMangaSeriesIdOrderByChapterNumberAsc(31L)).thenReturn(java.util.List.of(
                com.mangastudio.backend.entity.Chapter.builder()
                        .id(301L).mangaSeries(series).chapterNumber(1).title("Chapter 1").publishStatus("APPROVED").build()
        ));
        when(mangaSeriesRepository.save(series)).thenReturn(series);

        var response = service.updateSeriesStatus(31L, 2L, "REVIEWING");

        assertEquals("REVIEWING", response.getStatus());
        verify(boardVoteRepository).deleteByMangaSeriesId(31L);
        verify(mangaSeriesRepository).save(series);
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

    private MangaSeries series(Long id, User owner, String status) {
        return MangaSeries.builder()
                .id(id)
                .mangaka(owner)
                .title("Series " + id)
                .status(status)
                .build();
    }
}
