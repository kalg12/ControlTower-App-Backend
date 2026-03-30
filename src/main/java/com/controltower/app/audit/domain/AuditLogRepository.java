package com.controltower.app.audit.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only repository for audit logs (no delete/update operations exposed).
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);

    @Query("""
        SELECT a FROM AuditLog a
        WHERE (:tenantId IS NULL OR a.tenantId = :tenantId)
          AND (:userId    IS NULL OR a.userId   = :userId)
          AND (:action    IS NULL OR a.action   = :action)
          AND (:from      IS NULL OR a.createdAt >= :from)
          AND (:to        IS NULL OR a.createdAt <= :to)
        ORDER BY a.createdAt DESC
        """)
    Page<AuditLog> findFiltered(
        UUID tenantId,
        UUID userId,
        AuditAction action,
        Instant from,
        Instant to,
        Pageable pageable
    );
}
