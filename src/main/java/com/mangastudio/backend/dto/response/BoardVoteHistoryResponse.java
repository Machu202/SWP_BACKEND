package com.mangastudio.backend.dto.response;

import java.time.LocalDateTime;

public class BoardVoteHistoryResponse {
    private final Long voteId;
    private final Long seriesId;
    private final String seriesTitle;
    private final String coverImageUrl;
    private final String genre;
    private final String summary;
    private final String description;
    private final String seriesStatus;
    private final Boolean isApproved;
    private final LocalDateTime votedAt;

    public BoardVoteHistoryResponse(Long voteId, Long seriesId, String seriesTitle,
                                    String coverImageUrl, String genre, String summary,
                                    String description, String seriesStatus,
                                    Boolean isApproved, LocalDateTime votedAt) {
        this.voteId = voteId;
        this.seriesId = seriesId;
        this.seriesTitle = seriesTitle;
        this.coverImageUrl = coverImageUrl;
        this.genre = genre;
        this.summary = summary;
        this.description = description;
        this.seriesStatus = seriesStatus;
        this.isApproved = isApproved;
        this.votedAt = votedAt;
    }

    public Long getVoteId() { return voteId; }
    public Long getSeriesId() { return seriesId; }
    public String getSeriesTitle() { return seriesTitle; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public String getGenre() { return genre; }
    public String getSummary() { return summary; }
    public String getDescription() { return description; }
    public String getSeriesStatus() { return seriesStatus; }
    public Boolean getIsApproved() { return isApproved; }
    public LocalDateTime getVotedAt() { return votedAt; }
}
