package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.BoardVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface BoardVoteRepository extends JpaRepository<BoardVote, Long> {
    
    // Finds a specific member's vote for a project.
    Optional<BoardVote> findByMangaSeriesIdAndBoardMemberId(Long seriesId, Long boardMemberId);

    @Query("SELECT vote FROM BoardVote vote JOIN FETCH vote.mangaSeries "
            + "WHERE vote.boardMember.id = :memberId ORDER BY vote.createdAt DESC, vote.id DESC")
    List<BoardVote> findCurrentVotesForMember(@Param("memberId") Long memberId);

    @Query("SELECT vote FROM BoardVote vote JOIN FETCH vote.mangaSeries JOIN FETCH vote.boardMember "
            + "ORDER BY vote.createdAt DESC, vote.id DESC")
    List<BoardVote> findAllCurrentVotesWithDetails();
    
    // Counts every vote for a project.
    long countByMangaSeriesId(Long seriesId);
    
    // Counts votes by decision (approved or rejected).
    long countByMangaSeriesIdAndIsApproved(Long seriesId, Boolean isApproved);

    // Clear stale votes before a rejected series enters a new Board review cycle.
    void deleteByMangaSeriesId(Long seriesId);
}
