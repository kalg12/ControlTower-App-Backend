package com.controltower.app.chat.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, UUID> {

    Optional<ChatConversation> findByVisitorToken(UUID visitorToken);

    Optional<ChatConversation> findByIdAndDeletedAtIsNull(UUID id);

    @Query("""
        SELECT c FROM ChatConversation c
        WHERE c.tenantId = :tenantId
          AND c.deletedAt IS NULL
          AND (:status IS NULL OR c.status = :status)
          AND (:agentId IS NULL OR c.agentId = :agentId)
        """)
    Page<ChatConversation> findFiltered(
            @Param("tenantId") UUID tenantId,
            @Param("status") ConversationStatus status,
            @Param("agentId") UUID agentId,
            Pageable pageable);

    @Query("""
        SELECT COUNT(c) FROM ChatConversation c
        WHERE c.tenantId = :tenantId
          AND c.status = com.controltower.app.chat.domain.ConversationStatus.WAITING
          AND c.deletedAt IS NULL
        """)
    long countWaiting(@Param("tenantId") UUID tenantId);

    @Query("""
        SELECT c FROM ChatConversation c
        WHERE c.status = com.controltower.app.chat.domain.ConversationStatus.CLOSED
          AND c.deletedAt IS NULL
          AND c.closedAt < :cutoff
        """)
    List<ChatConversation> findClosedOlderThan(@Param("cutoff") Instant cutoff);
}
