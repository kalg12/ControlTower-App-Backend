package com.controltower.app.email.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface EmailRawRepository extends JpaRepository<EmailRaw, UUID> {

    Optional<EmailRaw> findByMessageId(String messageId);

    boolean existsByMessageId(String messageId);

    Page<EmailRaw> findByTenantId(UUID tenantId, Pageable pageable);

    /** Find most recent email with this message-id for thread detection. */
    @Query("""
        SELECT e FROM EmailRaw e
        WHERE e.tenantId = :tenantId
          AND (e.messageId = :replyTo OR e.messageId = :inReplyTo)
        ORDER BY e.receivedAt DESC
        """)
    Optional<EmailRaw> findByTenantIdAndReplyChain(
        @Param("tenantId") UUID tenantId,
        @Param("replyTo") String replyTo,
        @Param("inReplyTo") String inReplyTo
    );
}
