package com.controltower.app.licenses.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LicenseRepository extends JpaRepository<License, UUID> {

    Optional<License> findByClientIdAndDeletedAtIsNull(UUID clientId);

    Page<License> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    long countByTenantIdAndStatusAndDeletedAtIsNull(UUID tenantId, License.LicenseStatus status);

    /** Licenses that are about to expire (for grace period transition). */
    @Query("""
        SELECT l FROM License l
        WHERE l.deletedAt IS NULL
          AND l.status IN ('TRIAL', 'ACTIVE')
          AND l.currentPeriodEnd <= :threshold
        """)
    List<License> findExpiring(Instant threshold);

    /** Licenses in grace period that have passed the grace end date (for suspension). */
    @Query("""
        SELECT l FROM License l
        WHERE l.deletedAt IS NULL
          AND l.status = 'GRACE'
          AND l.gracePeriodEnd <= :now
        """)
    List<License> findGraceExpired(Instant now);

    @Query("""
        SELECT l FROM License l
        WHERE l.deletedAt IS NULL
          AND l.status IN ('TRIAL', 'ACTIVE')
          AND l.currentPeriodEnd >= :from
          AND l.currentPeriodEnd < :to
        """)
    List<License> findExpiringBetween(Instant from, Instant to);
}
