package com.controltower.app.email.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailTicketLinkRepository extends JpaRepository<EmailTicketLink, UUID> {

    List<EmailTicketLink> findByTicketId(UUID ticketId);

    List<EmailTicketLink> findByEmailId(UUID emailId);

    /** Returns the ticket linked to a given email if one exists. */
    @Query("SELECT l.ticketId FROM EmailTicketLink l WHERE l.emailId = :emailId")
    Optional<UUID> findTicketIdByEmailId(@Param("emailId") UUID emailId);

    /** Returns the most recent ticket linked to any email in a thread. */
    @Query("""
        SELECT l.ticketId FROM EmailTicketLink l
        INNER JOIN EmailRaw e ON e.id = l.emailId
        WHERE e.tenantId = :tenantId
          AND (e.messageId IN :messageIds)
        ORDER BY e.receivedAt DESC
        """)
    List<UUID> findTicketIdsByMessageIds(
        @Param("tenantId") UUID tenantId,
        @Param("messageIds") List<String> messageIds
    );
}
