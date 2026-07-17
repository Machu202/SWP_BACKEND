package com.mangastudio.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Immutable audit entry for every Editorial Board vote action. */
@Entity
@Table(name = "Board_Vote_History")
public class BoardVoteHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id", nullable = false)
    private MangaSeries mangaSeries;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_member_id", nullable = false)
    private User boardMember;

    @Column(name = "is_approved", nullable = false)
    private Boolean isApproved;

    @Column(name = "voted_at", nullable = false)
    private LocalDateTime votedAt;

    public BoardVoteHistory() {}

    public BoardVoteHistory(Long id, MangaSeries mangaSeries, User boardMember,
                            Boolean isApproved, LocalDateTime votedAt) {
        this.id = id;
        this.mangaSeries = mangaSeries;
        this.boardMember = boardMember;
        this.isApproved = isApproved;
        this.votedAt = votedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public MangaSeries getMangaSeries() { return mangaSeries; }
    public void setMangaSeries(MangaSeries mangaSeries) { this.mangaSeries = mangaSeries; }
    public User getBoardMember() { return boardMember; }
    public void setBoardMember(User boardMember) { this.boardMember = boardMember; }
    public Boolean getIsApproved() { return isApproved; }
    public void setIsApproved(Boolean approved) { isApproved = approved; }
    public LocalDateTime getVotedAt() { return votedAt; }
    public void setVotedAt(LocalDateTime votedAt) { this.votedAt = votedAt; }
}
