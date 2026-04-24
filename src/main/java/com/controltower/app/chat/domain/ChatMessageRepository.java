package com.controltower.app.chat.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    Page<ChatMessage> findByConversationIdOrderByCreatedAtAsc(UUID conversationId, Pageable pageable);

    List<ChatMessage> findTop50ByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.conversation.id = :conversationId AND m.isRead = FALSE AND m.senderType <> com.controltower.app.chat.domain.SenderType.AGENT")
    long countUnreadByConversationId(@Param("conversationId") UUID conversationId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = TRUE WHERE m.conversation.id = :conversationId AND m.isRead = FALSE")
    int markAllReadByConversationId(@Param("conversationId") UUID conversationId);
}
