package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.BoardVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface BoardVoteRepository extends JpaRepository<BoardVote, Long> {
    
    // Tìm lá phiếu của một thành viên cụ thể trong một dự án
    Optional<BoardVote> findByMangaSeriesIdAndBoardMemberId(Long seriesId, Long boardMemberId);

    @Query("SELECT vote FROM BoardVote vote JOIN FETCH vote.mangaSeries "
            + "WHERE vote.boardMember.id = :memberId ORDER BY vote.createdAt DESC, vote.id DESC")
    List<BoardVote> findCurrentVotesForMember(@Param("memberId") Long memberId);
    
    // Đếm tổng số phiếu của dự án
    long countByMangaSeriesId(Long seriesId);
    
    // Đếm số phiếu theo trạng thái (Đồng ý / Từ chối)
    long countByMangaSeriesIdAndIsApproved(Long seriesId, Boolean isApproved);

    // Clear stale votes before a rejected series enters a new Board review cycle.
    void deleteByMangaSeriesId(Long seriesId);
}
