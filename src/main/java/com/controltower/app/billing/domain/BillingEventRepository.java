package com.controltower.app.billing.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BillingEventRepository extends JpaRepository<BillingEvent, UUID> {

    Page<BillingEvent> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
}
