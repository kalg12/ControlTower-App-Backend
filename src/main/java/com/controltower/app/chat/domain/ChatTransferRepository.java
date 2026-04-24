package com.controltower.app.chat.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatTransferRepository extends JpaRepository<ChatTransfer, UUID> {

    List<ChatTransfer> findByConversationIdOrderByTransferredAtDesc(UUID conversationId);
}
