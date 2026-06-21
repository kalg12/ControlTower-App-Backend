package com.controltower.app.email.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailRoutingRuleRepository extends JpaRepository<EmailRoutingRule, UUID> {

    Optional<EmailRoutingRule> findByIdAndTenantId(UUID id, UUID tenantId);

    List<EmailRoutingRule> findByTenantId(UUID tenantId);

    @Query("""
        SELECT r FROM EmailRoutingRule r
        WHERE r.tenantId = :tenantId
          AND r.active = true
          AND (r.aliasId IS NULL OR r.aliasId = :aliasId)
        ORDER BY r.priority ASC
        """)
    List<EmailRoutingRule> findActiveRulesForAlias(
        @Param("tenantId") UUID tenantId,
        @Param("aliasId") UUID aliasId
    );
}
