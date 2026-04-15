package com.controltower.app.time.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TimeEntryRepository extends JpaRepository<TimeEntry, UUID> {

    /** Active timer for a specific user in a tenant (at most one per user). */
    @Query("""
        SELECT t FROM TimeEntry t
        WHERE t.tenantId = :tenantId
          AND t.userId   = :userId
          AND t.endedAt  IS NULL
          AND t.deletedAt IS NULL
        """)
    Optional<TimeEntry> findActiveByTenantAndUser(
            @Param("tenantId") UUID tenantId,
            @Param("userId")   UUID userId);

    /** All non-deleted entries for a specific entity (ticket or card). */
    @Query("""
        SELECT t FROM TimeEntry t
        WHERE t.entityType  = :entityType
          AND t.entityId    = :entityId
          AND t.deletedAt   IS NULL
        ORDER BY t.startedAt DESC
        """)
    List<TimeEntry> findByEntityTypeAndEntityId(
            @Param("entityType") TimeEntry.EntityType entityType,
            @Param("entityId")   UUID entityId);

    /** Sum of logged minutes for a specific entity (excludes active/running entries). */
    @Query("""
        SELECT COALESCE(SUM(t.minutes), 0)
        FROM TimeEntry t
        WHERE t.entityType = :entityType
          AND t.entityId   = :entityId
          AND t.endedAt    IS NOT NULL
          AND t.deletedAt  IS NULL
        """)
    int sumMinutesByEntityTypeAndEntityId(
            @Param("entityType") TimeEntry.EntityType entityType,
            @Param("entityId")   UUID entityId);

    /** Paginated entries for a tenant (for analytics). */
    @Query("""
        SELECT t FROM TimeEntry t
        WHERE t.tenantId    = :tenantId
          AND t.startedAt  >= :from
          AND t.startedAt  <= :to
          AND t.deletedAt   IS NULL
        """)
    Page<TimeEntry> findByTenantAndPeriod(
            @Param("tenantId") UUID tenantId,
            @Param("from")     Instant from,
            @Param("to")       Instant to,
            Pageable pageable);

    /** Sum of logged minutes per user in a tenant for analytics. */
    @Query("""
        SELECT t.userId, COALESCE(SUM(t.minutes), 0)
        FROM TimeEntry t
        WHERE t.tenantId    = :tenantId
          AND t.startedAt  >= :from
          AND t.endedAt    IS NOT NULL
          AND t.deletedAt   IS NULL
        GROUP BY t.userId
        ORDER BY SUM(t.minutes) DESC
        """)
    List<Object[]> sumMinutesPerUser(
            @Param("tenantId") UUID tenantId,
            @Param("from")     Instant from);
}
