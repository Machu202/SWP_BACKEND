package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.BoardVoteHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BoardVoteHistoryRepository extends JpaRepository<BoardVoteHistory, Long> {

    @Query("SELECT history FROM BoardVoteHistory history "
            + "JOIN FETCH history.mangaSeries series "
            + "WHERE history.boardMember.id = :memberId "
            + "ORDER BY history.votedAt DESC, history.id DESC")
    List<BoardVoteHistory> findHistoryForMember(@Param("memberId") Long memberId);
}
