package com.mangastudio.backend.service;

import com.mangastudio.backend.dto.response.BoardVoteSummaryResponse;
import com.mangastudio.backend.dto.response.BoardVoteHistoryResponse;
import com.mangastudio.backend.dto.response.AdminBoardVoteHistoryResponse;
import com.mangastudio.backend.entity.BoardVote;

import java.util.List;

public interface BoardVoteService {
    // Hàm dành cho Hội đồng biên tập bỏ phiếu
    BoardVote castVote(Long seriesId, Long memberId, boolean isApproved);
    
    // Hàm dành cho Admin lấy thống kê để quyết định
    BoardVoteSummaryResponse getVoteSummary(Long seriesId);

    List<BoardVoteHistoryResponse> getMyVoteHistory(Long memberId);

    List<AdminBoardVoteHistoryResponse> getAdminVoteHistory();
}
