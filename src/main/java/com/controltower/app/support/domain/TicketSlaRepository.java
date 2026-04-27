package com.controltower.app.support.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface TicketSlaRepository extends JpaRepository<TicketSla, UUID> {

    /** Returns all unbreached SLAs that are now past due, only for open tickets (for scheduler).
     *  Excludes RESOLVED/CLOSED tickets — a ticket resolved before the deadline must not be marked breached. */
    @Query("""
        SELECT s FROM TicketSla s
        WHERE s.breached = false
          AND s.dueAt <= :now
          AND s.ticket.deletedAt IS NULL
          AND s.ticket.status NOT IN ('RESOLVED', 'CLOSED')
        """)
    List<TicketSla> findBreachedUnmarked(Instant now);

    /** Returns all active SLAs for non-resolved, non-closed tickets (used by SLA warning scheduler). */
    @Query("""
        SELECT s FROM TicketSla s
        WHERE s.breached = false
          AND s.ticket.deletedAt IS NULL
          AND s.ticket.status NOT IN ('RESOLVED', 'CLOSED')
        """)
    List<TicketSla> findAllActive();

    /** Count breached SLAs linked to open tickets of a given tenant. */
    @Query("""
        SELECT COUNT(s) FROM TicketSla s
        WHERE s.ticket.tenantId = :tenantId
          AND s.ticket.deletedAt IS NULL
          AND s.ticket.status NOT IN ('RESOLVED', 'CLOSED')
          AND s.breached = true
        """)
    long countActiveBreachedByTenant(UUID tenantId);
}
