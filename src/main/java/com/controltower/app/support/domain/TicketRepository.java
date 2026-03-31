package com.controltower.app.support.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Page<Ticket> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    long countByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, Ticket.TicketStatus status);

    Optional<Ticket> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    @Query("""
        SELECT t FROM Ticket t
        WHERE t.tenantId = :tenantId
          AND t.deletedAt IS NULL
          AND (:status IS NULL OR t.status = :status)
          AND (:assigneeId IS NULL OR t.assigneeId = :assigneeId)
          AND (:clientId IS NULL OR t.clientId = :clientId)
        ORDER BY t.createdAt DESC
        """)
    Page<Ticket> findFiltered(
        UUID tenantId,
        Ticket.TicketStatus status,
        UUID assigneeId,
        UUID clientId,
        Pageable pageable
    );
}
