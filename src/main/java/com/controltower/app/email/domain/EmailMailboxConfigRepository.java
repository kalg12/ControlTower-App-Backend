package com.controltower.app.email.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailMailboxConfigRepository extends JpaRepository<EmailMailboxConfig, UUID> {

    List<EmailMailboxConfig> findByTenantIdAndActiveTrue(UUID tenantId);

    Optional<EmailMailboxConfig> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("""
        SELECT m FROM EmailMailboxConfig m
        WHERE m.active = true
          AND (m.lastPolledAt IS NULL
               OR m.lastPolledAt < :cutoff)
        """)
    List<EmailMailboxConfig> findDueForPoll(@Param("cutoff") Instant cutoff);
}
