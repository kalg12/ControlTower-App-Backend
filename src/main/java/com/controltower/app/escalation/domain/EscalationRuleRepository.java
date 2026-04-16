package com.controltower.app.escalation.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EscalationRuleRepository extends JpaRepository<EscalationRule, UUID> {
    List<EscalationRule> findByTenantId(UUID tenantId);
}
