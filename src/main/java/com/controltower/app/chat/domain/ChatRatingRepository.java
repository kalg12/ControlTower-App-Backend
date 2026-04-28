package com.controltower.app.chat.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ChatRatingRepository extends JpaRepository<ChatRating, UUID> {
    Optional<ChatRating> findByConversationId(UUID conversationId);
    boolean existsByConversationId(UUID conversationId);
}
