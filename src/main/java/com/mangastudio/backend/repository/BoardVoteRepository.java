package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.BoardVote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardVoteRepository extends JpaRepository<BoardVote, Long> {
}