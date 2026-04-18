package com.controltower.app.identity.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailAndTenantIdAndDeletedAtIsNull(String email, UUID tenantId);

    Optional<User> findByIdAndDeletedAtIsNull(UUID id);

    /** Used during login: find by email across all tenants (email + tenant pair is unique). */
    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Page<User> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    boolean existsByEmailAndTenantIdAndDeletedAtIsNull(String email, UUID tenantId);

    @Query("""
        SELECT DISTINCT u FROM User u
        JOIN u.roles r
        JOIN r.permissions p
        WHERE u.tenantId = :tenantId
          AND u.deletedAt IS NULL
          AND p.code = :permissionCode
        """)
    List<User> findByTenantIdAndPermission(
            @Param("tenantId") UUID tenantId,
            @Param("permissionCode") String permissionCode);
}
