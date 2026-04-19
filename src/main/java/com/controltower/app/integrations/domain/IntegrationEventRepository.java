package com.controltower.app.integrations.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IntegrationEventRepository extends JpaRepository<IntegrationEvent, UUID> {

    Page<IntegrationEvent> findByEndpointIdOrderByReceivedAtDesc(UUID endpointId, Pageable pageable);
}
