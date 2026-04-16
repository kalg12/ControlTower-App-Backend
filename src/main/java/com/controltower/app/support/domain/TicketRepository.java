package com.controltower.app.support.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID>, JpaSpecificationExecutor<Ticket> {

    Page<Ticket> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    long countByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, Ticket.TicketStatus status);

    long countByTenantIdAndDeletedAtIsNull(UUID tenantId);

    Optional<Ticket> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    List<Ticket> findByIdInAndTenantIdAndDeletedAtIsNull(List<UUID> ids, UUID tenantId);

    @Query("""
        SELECT t FROM Ticket t
        WHERE t.tenantId = :tenantId
          AND t.deletedAt IS NULL
          AND (:status IS NULL OR t.status = :status)
          AND (:assigneeId IS NULL OR t.assigneeId = :assigneeId)
          AND (:clientId IS NULL OR t.clientId = :clientId)
          AND (:priority IS NULL OR t.priority = :priority)
          AND (:createdAfter IS NULL OR t.createdAt >= :createdAfter)
          AND (:createdBefore IS NULL OR t.createdAt <= :createdBefore)
        ORDER BY t.createdAt DESC
        """)
    Page<Ticket> findFiltered(
        @Param("tenantId")     UUID tenantId,
        @Param("status")       Ticket.TicketStatus status,
        @Param("assigneeId")   UUID assigneeId,
        @Param("clientId")     UUID clientId,
        @Param("priority")     Ticket.Priority priority,
        @Param("createdAfter") Instant createdAfter,
        @Param("createdBefore") Instant createdBefore,
        Pageable pageable
    );

    /** Tickets whose SLA is not yet breached but will expire before :threshold (SLA at risk). */
    @Query("""
        SELECT t FROM Ticket t
        JOIN TicketSla s ON s.ticket = t
        WHERE t.tenantId = :tenantId
          AND t.deletedAt IS NULL
          AND t.status NOT IN ('RESOLVED', 'CLOSED')
          AND s.breached = false
          AND s.dueAt <= :threshold
        ORDER BY s.dueAt ASC
        """)
    Page<Ticket> findSlaAtRisk(
        @Param("tenantId")  UUID tenantId,
        @Param("threshold") Instant threshold,
        Pageable pageable
    );

    Optional<Ticket> findBySourceRefIdAndTenantIdAndDeletedAtIsNull(String sourceRefId, UUID tenantId);

    long countByTenantIdAndSourceAndDeletedAtIsNull(UUID tenantId, Ticket.TicketSource source);

    @Query("""
        SELECT t.status, COUNT(t) FROM Ticket t
        WHERE t.tenantId = :tenantId
          AND t.deletedAt IS NULL
          AND t.source = :source
        GROUP BY t.status
        """)
    List<Object[]> countByStatusForSource(
        @Param("tenantId") UUID tenantId,
        @Param("source") Ticket.TicketSource source
    );

    /**
     * Returns pairs of [assigneeId, openCount] for all users who have at least one
     * active (non-resolved, non-closed, non-deleted) ticket in the given tenant.
     * Used to determine workload when auto-assigning tickets.
     */
    @Query("""
        SELECT t.assigneeId, COUNT(t) FROM Ticket t
        WHERE t.tenantId = :tenantId
          AND t.deletedAt IS NULL
          AND t.assigneeId IS NOT NULL
          AND t.status NOT IN ('RESOLVED', 'CLOSED')
        GROUP BY t.assigneeId
        """)
    List<Object[]> countOpenTicketsPerAssignee(@Param("tenantId") UUID tenantId);

    /** Used for CSV export — no pagination. */
    @Query("""
        SELECT t FROM Ticket t
        WHERE t.tenantId = :tenantId
          AND t.deletedAt IS NULL
          AND (:status IS NULL OR t.status = :status)
          AND (:assigneeId IS NULL OR t.assigneeId = :assigneeId)
          AND (:clientId IS NULL OR t.clientId = :clientId)
          AND (:priority IS NULL OR t.priority = :priority)
          AND (:createdAfter IS NULL OR t.createdAt >= :createdAfter)
          AND (:createdBefore IS NULL OR t.createdAt <= :createdBefore)
        ORDER BY t.createdAt DESC
        """)
    List<Ticket> findAllForExport(
        @Param("tenantId")      UUID tenantId,
        @Param("status")        Ticket.TicketStatus status,
        @Param("assigneeId")    UUID assigneeId,
        @Param("clientId")      UUID clientId,
        @Param("priority")      Ticket.Priority priority,
        @Param("createdAfter")  Instant createdAfter,
        @Param("createdBefore") Instant createdBefore
    );
}
