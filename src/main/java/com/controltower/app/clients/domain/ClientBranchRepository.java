package com.controltower.app.clients.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClientBranchRepository extends JpaRepository<ClientBranch, UUID> {

    List<ClientBranch> findByClientIdAndDeletedAtIsNull(UUID clientId);

    Optional<ClientBranch> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    Optional<ClientBranch> findBySlugAndDeletedAtIsNull(String slug);
}
