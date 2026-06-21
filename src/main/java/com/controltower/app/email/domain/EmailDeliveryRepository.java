package com.controltower.app.email.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailDeliveryRepository extends JpaRepository<EmailDelivery, UUID> {

    Page<EmailDelivery> findByTenantId(UUID tenantId, Pageable pageable);

    Optional<EmailDelivery> findByIdAndTenantId(UUID id, UUID tenantId);

    List<EmailDelivery> findByTicketId(UUID ticketId);

    @Query("""
        SELECT d FROM EmailDelivery d
        WHERE d.status = 'QUEUED'
          AND d.attempts > 0
          AND d.nextRetryAt <= :now
        """)
    List<EmailDelivery> findDueForRetry(@Param("now") Instant now);
}
