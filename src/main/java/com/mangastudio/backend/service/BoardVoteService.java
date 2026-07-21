package com.mangastudio.backend.service;

import com.mangastudio.backend.dto.response.BoardVoteSummaryResponse;
import com.mangastudio.backend.dto.response.BoardVoteHistoryResponse;
import com.mangastudio.backend.dto.response.AdminBoardVoteHistoryResponse;
import com.mangastudio.backend.entity.BoardVote;

import java.util.List;

public interface BoardVoteService {
    // Allows an Editorial Board member to cast a vote.
    BoardVote castVote(Long seriesId, Long memberId, boolean isApproved);
    
    // Provides vote statistics for the Admin's final decision.
    BoardVoteSummaryResponse getVoteSummary(Long seriesId);

    List<BoardVoteHistoryResponse> getMyVoteHistory(Long memberId);

    List<AdminBoardVoteHistoryResponse> getAdminVoteHistory();
}
