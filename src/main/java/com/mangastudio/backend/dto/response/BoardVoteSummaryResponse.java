package com.mangastudio.backend.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BoardVoteSummaryResponse {
    private Long seriesId;
    private long totalVotes;
    private long approvedVotes;
    private long rejectedVotes;
}