package com.controltower.app.health.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
