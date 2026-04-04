package com.controltower.app.campaigns.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, UUID>, JpaSpecificationExecutor<Campaign> {


    Optional<Campaign> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);
}
