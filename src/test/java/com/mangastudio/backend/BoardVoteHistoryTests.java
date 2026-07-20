package com.mangastudio.backend;

import com.mangastudio.backend.entity.BoardVote;
import com.mangastudio.backend.entity.BoardVoteHistory;
import com.mangastudio.backend.entity.MangaSeries;
import com.mangastudio.backend.entity.Role;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.BoardVoteHistoryRepository;
import com.mangastudio.backend.repository.BoardVoteRepository;
import com.mangastudio.backend.repository.MangaSeriesRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.impl.BoardVoteServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BoardVoteHistoryTests {

    @Test
    void everyVoteCreatesADurablePersonalHistoryEntry() {
        BoardVoteRepository votes = mock(BoardVoteRepository.class);
        BoardVoteHistoryRepository history = mock(BoardVoteHistoryRepository.class);
        MangaSeriesRepository seriesRepository = mock(MangaSeriesRepository.class);
        UserRepository users = mock(UserRepository.class);
        BoardVoteServiceImpl service = new BoardVoteServiceImpl(votes, history, seriesRepository, users);

        User member = User.builder().id(7L).role(Role.builder().roleName("Editorial Board").build()).build();
        MangaSeries series = MangaSeries.builder().id(11L).title("Doraemon").status("REVIEWING").build();
        when(seriesRepository.findById(11L)).thenReturn(Optional.of(series));
        when(users.findById(7L)).thenReturn(Optional.of(member));
        when(votes.findByMangaSeriesIdAndBoardMemberId(11L, 7L)).thenReturn(Optional.empty());
        when(votes.save(any(BoardVote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.castVote(11L, 7L, false);

        ArgumentCaptor<BoardVoteHistory> entry = ArgumentCaptor.forClass(BoardVoteHistory.class);
        verify(history).save(entry.capture());
        assertEquals(11L, entry.getValue().getMangaSeries().getId());
        assertEquals(7L, entry.getValue().getBoardMember().getId());
        assertFalse(entry.getValue().getIsApproved());
    }

    @Test
    void personalHistoryContainsSeriesDetails() {
        BoardVoteRepository votes = mock(BoardVoteRepository.class);
        BoardVoteHistoryRepository history = mock(BoardVoteHistoryRepository.class);
        MangaSeriesRepository seriesRepository = mock(MangaSeriesRepository.class);
        UserRepository users = mock(UserRepository.class);
        BoardVoteServiceImpl service = new BoardVoteServiceImpl(votes, history, seriesRepository, users);

        User member = User.builder().id(7L).role(Role.builder().roleName("Editorial Board").build()).build();
        MangaSeries series = MangaSeries.builder().id(11L).title("Doraemon").summary("Robot cat").status("APPROVED").build();
        BoardVoteHistory entry = new BoardVoteHistory(50L, series, member, true, java.time.LocalDateTime.now());
        when(users.findById(7L)).thenReturn(Optional.of(member));
        when(history.findHistoryForMember(7L)).thenReturn(List.of(entry));
        when(votes.findCurrentVotesForMember(7L)).thenReturn(List.of());

        var result = service.getMyVoteHistory(7L);

        assertEquals(1, result.size());
        assertEquals("Doraemon", result.get(0).getSeriesTitle());
        assertEquals("Robot cat", result.get(0).getSummary());
        assertEquals(true, result.get(0).getIsApproved());
    }

    @Test
    void adminHistoryIncludesBoardMemberDecisionAndSeries() {
        BoardVoteRepository votes = mock(BoardVoteRepository.class);
        BoardVoteHistoryRepository history = mock(BoardVoteHistoryRepository.class);
        MangaSeriesRepository seriesRepository = mock(MangaSeriesRepository.class);
        UserRepository users = mock(UserRepository.class);
        BoardVoteServiceImpl service = new BoardVoteServiceImpl(votes, history, seriesRepository, users);

        User member = User.builder().id(7L).username("nori").fullName("Nori Board")
                .role(Role.builder().roleName("Editorial Board").build()).build();
        MangaSeries series = MangaSeries.builder().id(11L).title("Doraemon").status("REVIEWING").build();
        BoardVoteHistory entry = new BoardVoteHistory(50L, series, member, false, java.time.LocalDateTime.now());
        when(history.findAllHistoryWithDetails()).thenReturn(List.of(entry));
        when(votes.findAllCurrentVotesWithDetails()).thenReturn(List.of());

        var result = service.getAdminVoteHistory();

        assertEquals(1, result.size());
        assertEquals("Nori Board", result.get(0).getBoardMemberName());
        assertEquals("Doraemon", result.get(0).getSeriesTitle());
        assertFalse(result.get(0).getIsApproved());
    }
}
