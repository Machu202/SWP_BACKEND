package com.mangastudio.backend.service;

import com.mangastudio.backend.dto.response.BoardVoteSummaryResponse;
import com.mangastudio.backend.entity.BoardVote;

public interface BoardVoteService {
    // Hàm dành cho Hội đồng biên tập bỏ phiếu
    BoardVote castVote(Long seriesId, Long memberId, boolean isApproved);
    
    // Hàm dành cho Admin lấy thống kê để quyết định
    BoardVoteSummaryResponse getVoteSummary(Long seriesId);
}