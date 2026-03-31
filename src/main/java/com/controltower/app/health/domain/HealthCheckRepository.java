package com.controltower.app.health.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface HealthCheckRepository extends JpaRepository<HealthCheck, UUID> {

    Page<HealthCheck> findByBranchIdOrderByCheckedAtDesc(UUID branchId, Pageable pageable);

    Page<HealthCheck> findByTenantIdOrderByCheckedAtDesc(UUID tenantId, Pageable pageable);

    /** Returns the N most recent checks for a branch (used for incident detection). */
    List<HealthCheck> findTop10ByBranchIdOrderByCheckedAtDesc(UUID branchId);

    /** Returns the latest check for each branch in a tenant (dashboard overview). */
    @Query("""
        SELECT h FROM HealthCheck h
        WHERE h.tenantId = :tenantId
          AND h.checkedAt = (
              SELECT MAX(h2.checkedAt) FROM HealthCheck h2
              WHERE h2.branchId = h.branchId
          )
        """)
    List<HealthCheck> findLatestPerBranch(UUID tenantId);

    /** Counts checks with a given status after a threshold time (for snapshot rollups). */
    long countByBranchIdAndStatusAndCheckedAtAfter(UUID branchId, HealthCheck.HealthStatus status, Instant after);

    long countByBranchIdAndCheckedAtAfter(UUID branchId, Instant after);

    @Query("SELECT h FROM HealthCheck h WHERE h.branchId = :branchId AND h.checkedAt >= :from AND h.checkedAt < :to")
    List<HealthCheck> findByBranchAndDateRange(@Param("branchId") UUID branchId,
                                               @Param("from") Instant from,
                                               @Param("to") Instant to);

    @Query("SELECT DISTINCT h.branchId FROM HealthCheck h WHERE h.checkedAt >= :from AND h.checkedAt < :to")
    List<UUID> findActiveBranchIds(@Param("from") Instant from, @Param("to") Instant to);

    @Query("SELECT h.tenantId FROM HealthCheck h WHERE h.branchId = :branchId AND h.checkedAt >= :from AND h.checkedAt < :to ORDER BY h.checkedAt DESC")
    List<UUID> findTenantIdForBranch(@Param("branchId") UUID branchId,
                                     @Param("from") Instant from,
                                     @Param("to") Instant to,
                                     Pageable pageable);
}
