package com.controltower.app.email.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailAliasRepository extends JpaRepository<EmailAlias, UUID> {

    List<EmailAlias> findByTenantIdAndActiveTrue(UUID tenantId);

    Optional<EmailAlias> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<EmailAlias> findByTenantIdAndAlias(UUID tenantId, String alias);

    List<EmailAlias> findByMailboxId(UUID mailboxId);
}
