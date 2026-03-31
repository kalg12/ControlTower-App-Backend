package com.controltower.app.support.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface TicketSlaRepository extends JpaRepository<TicketSla, UUID> {

    /** Returns all unbreached SLAs that are now past due (for scheduler). */
    @Query("SELECT s FROM TicketSla s WHERE s.breached = false AND s.dueAt <= :now")
    List<TicketSla> findBreachedUnmarked(Instant now);

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
