package com.mangastudio.backend.repository;

import com.mangastudio.backend.entity.DirectChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.LocalDateTime;

public interface DirectChatMessageRepository extends JpaRepository<DirectChatMessage, Long> {

    @Query("SELECT message FROM DirectChatMessage message "
            + "JOIN FETCH message.sender JOIN FETCH message.recipient "
            + "WHERE (message.sender.id = :currentUserId AND message.recipient.id = :otherUserId) "
            + "OR (message.sender.id = :otherUserId AND message.recipient.id = :currentUserId) "
            + "ORDER BY message.createdAt ASC, message.id ASC")
    List<DirectChatMessage> findConversation(@Param("currentUserId") Long currentUserId,
                                             @Param("otherUserId") Long otherUserId);

    long countByRecipient_IdAndSender_IdAndReadAtIsNull(Long recipientId, Long senderId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE DirectChatMessage message SET message.readAt = :readAt "
            + "WHERE message.recipient.id = :currentUserId AND message.sender.id = :otherUserId "
            + "AND message.readAt IS NULL")
    int markConversationRead(@Param("currentUserId") Long currentUserId,
                             @Param("otherUserId") Long otherUserId,
                             @Param("readAt") LocalDateTime readAt);
}
