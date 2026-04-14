package com.controltower.app.activity.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface UserActivityRepository
        extends JpaRepository<UserActivity, UUID>,
                JpaSpecificationExecutor<UserActivity> {

    @Query("""
        SELECT a FROM UserActivity a
        WHERE  a.tenant.id = :tenantId
        AND    a.userId    = :userId
        AND    a.deletedAt IS NULL
        ORDER BY a.visitedAt DESC
        LIMIT 50
        """)
    List<UserActivity> findRecentByUser(
            @Param("tenantId") UUID tenantId,
            @Param("userId")   UUID userId);

    @Query("""
        SELECT a FROM UserActivity a
        WHERE  a.tenant.id  = :tenantId
        AND    a.deletedAt  IS NULL
        AND    a.visitedAt >= :since
        ORDER BY a.visitedAt DESC
        """)
    List<UserActivity> findActiveSince(
            @Param("tenantId") UUID tenantId,
            @Param("since")    Instant since);

    long countByTenantIdAndDeletedAtIsNull(UUID tenantId);

    long countByTenantIdAndVisitedAtBetweenAndDeletedAtIsNull(UUID tenantId, Instant from, Instant to);
}
