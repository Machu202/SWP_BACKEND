package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.BoardChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BoardChatMessageRepository extends JpaRepository<BoardChatMessage, Long> {

    @Query("SELECT chat FROM BoardChatMessage chat JOIN FETCH chat.sender "
            + "WHERE chat.mangaSeries.id = :seriesId ORDER BY chat.createdAt ASC, chat.id ASC")
    List<BoardChatMessage> findRoomMessages(@Param("seriesId") Long seriesId);

    void deleteByMangaSeriesId(Long seriesId);
}
