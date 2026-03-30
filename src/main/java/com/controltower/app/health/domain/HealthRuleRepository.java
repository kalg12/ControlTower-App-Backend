package com.controltower.app.health.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HealthRuleRepository extends JpaRepository<HealthRule, UUID> {

    /** Returns all active rules that apply to a specific branch (branch-specific + tenant-wide). */
    @Query("""
        SELECT r FROM HealthRule r
        WHERE r.active = true
          AND r.tenantId = :tenantId
          AND (r.branchId = :branchId OR r.branchId IS NULL)
        """)
    List<HealthRule> findActiveRulesForBranch(UUID tenantId, UUID branchId);

    List<HealthRule> findByTenantIdAndActive(UUID tenantId, boolean active);
}
