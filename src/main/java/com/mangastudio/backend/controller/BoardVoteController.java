package com.mangastudio.backend.controller;

import com.mangastudio.backend.dto.response.BoardVoteHistoryResponse;
import com.mangastudio.backend.dto.response.AdminBoardVoteHistoryResponse;
import com.mangastudio.backend.dto.response.BoardVoteSummaryResponse;
import com.mangastudio.backend.entity.BoardVote;
import com.mangastudio.backend.security.UserDetailsImpl;
import com.mangastudio.backend.service.BoardVoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/votes")
public class BoardVoteController {

    private final BoardVoteService boardVoteService;

    public BoardVoteController(BoardVoteService boardVoteService) {
        this.boardVoteService = boardVoteService;
    }

    // API 1: Hội đồng biên tập thực hiện bỏ phiếu (Truyền tham số isApproved = true/false)
    @PostMapping("/series/{seriesId}")
    @PreAuthorize("hasAnyAuthority('ROLE_EDITORIAL_BOARD', 'ROLE_EDITORIAL BOARD', 'ROLE_ADMIN')")
    public ResponseEntity<BoardVote> castVote(
            @PathVariable Long seriesId,
            @RequestParam boolean isApproved,
            Authentication authentication) {
        
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        BoardVote vote = boardVoteService.castVote(seriesId, userDetails.getId(), isApproved);
        return ResponseEntity.ok(vote);
    }

    // API 2: Admin lấy báo cáo thống kê phiếu bầu
    @GetMapping("/series/{seriesId}/summary")
    @PreAuthorize("hasAnyAuthority('ROLE_EDITORIAL_BOARD', 'ROLE_EDITORIAL BOARD', 'ROLE_ADMIN')")
    public ResponseEntity<BoardVoteSummaryResponse> getVoteSummary(@PathVariable Long seriesId) {
        BoardVoteSummaryResponse summary = boardVoteService.getVoteSummary(seriesId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/my-history")
    @PreAuthorize("hasAnyAuthority('ROLE_EDITORIAL_BOARD', 'ROLE_EDITORIAL BOARD', 'ROLE_ADMIN')")
    public ResponseEntity<List<BoardVoteHistoryResponse>> getMyVoteHistory(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok(boardVoteService.getMyVoteHistory(userDetails.getId()));
    }

    @GetMapping("/admin/history")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<AdminBoardVoteHistoryResponse>> getAdminVoteHistory() {
        return ResponseEntity.ok(boardVoteService.getAdminVoteHistory());
    }
}
