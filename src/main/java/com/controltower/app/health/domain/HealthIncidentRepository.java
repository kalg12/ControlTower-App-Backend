package com.controltower.app.health.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HealthIncidentRepository extends JpaRepository<HealthIncident, UUID> {

    Page<HealthIncident> findByTenantIdOrderByOpenedAtDesc(UUID tenantId, Pageable pageable);

    Page<HealthIncident> findByBranchIdOrderByOpenedAtDesc(UUID branchId, Pageable pageable);

    /** Returns the currently open incident for a branch, if any. */
    Optional<HealthIncident> findByBranchIdAndResolvedAtIsNull(UUID branchId);

    long countByTenantIdAndResolvedAtIsNull(UUID tenantId);

    /**
     * Filterable incident log for the health dashboard.
     * - branchId = null  → all branches in the tenant
     * - openOnly  = true → only open (resolvedAt IS NULL)
     * - openOnly  = false → all (open + resolved)
     */
    @Query("""
            SELECT i FROM HealthIncident i
            WHERE i.tenantId = :tenantId
              AND (:branchId IS NULL OR i.branchId = :branchId)
              AND (:openOnly = false OR i.resolvedAt IS NULL)
            ORDER BY i.openedAt DESC
            """)
    Page<HealthIncident> findLog(
            @Param("tenantId")  UUID    tenantId,
            @Param("branchId")  UUID    branchId,
            @Param("openOnly")  boolean openOnly,
            Pageable pageable);
}
