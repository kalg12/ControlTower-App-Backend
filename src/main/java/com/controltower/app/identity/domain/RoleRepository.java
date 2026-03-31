package com.controltower.app.identity.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByCodeAndTenantId(String code, UUID tenantId);

    List<Role> findByTenantIdAndDeletedAtIsNull(UUID tenantId);

    Page<Role> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Optional<Role> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);
}
