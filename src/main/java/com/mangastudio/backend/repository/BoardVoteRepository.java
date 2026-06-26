package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.BoardVote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BoardVoteRepository extends JpaRepository<BoardVote, Long> {
    
    // Tìm lá phiếu của một thành viên cụ thể trong một dự án
    Optional<BoardVote> findByMangaSeriesIdAndBoardMemberId(Long seriesId, Long boardMemberId);
    
    // Đếm tổng số phiếu của dự án
    long countByMangaSeriesId(Long seriesId);
    
    // Đếm số phiếu theo trạng thái (Đồng ý / Từ chối)
    long countByMangaSeriesIdAndIsApproved(Long seriesId, Boolean isApproved);
}