package com.controltower.app.integrations.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IntegrationEventRepository extends JpaRepository<IntegrationEvent, UUID> {
}
