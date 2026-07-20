package com.mangastudio.backend.dto.response;

import java.time.LocalDateTime;

public class AdminBoardVoteHistoryResponse {
    private final Long voteId;
    private final Long boardMemberId;
    private final String boardMemberName;
    private final Long seriesId;
    private final String seriesTitle;
    private final String coverImageUrl;
    private final Boolean isApproved;
    private final LocalDateTime votedAt;

    public AdminBoardVoteHistoryResponse(Long voteId, Long boardMemberId, String boardMemberName,
                                         Long seriesId, String seriesTitle, String coverImageUrl,
                                         Boolean isApproved, LocalDateTime votedAt) {
        this.voteId = voteId;
        this.boardMemberId = boardMemberId;
        this.boardMemberName = boardMemberName;
        this.seriesId = seriesId;
        this.seriesTitle = seriesTitle;
        this.coverImageUrl = coverImageUrl;
        this.isApproved = isApproved;
        this.votedAt = votedAt;
    }

    public Long getVoteId() { return voteId; }
    public Long getBoardMemberId() { return boardMemberId; }
    public String getBoardMemberName() { return boardMemberName; }
    public Long getSeriesId() { return seriesId; }
    public String getSeriesTitle() { return seriesTitle; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public Boolean getIsApproved() { return isApproved; }
    public LocalDateTime getVotedAt() { return votedAt; }
}
