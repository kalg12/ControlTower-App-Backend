package com.controltower.app.identity.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query(value = """
        SELECT DISTINCT u.* FROM users u
        JOIN user_roles ur ON ur.user_id = u.id
        JOIN roles r ON r.id = ur.role_id
        JOIN role_permissions rp ON rp.role_id = r.id
        JOIN permissions p ON p.id = rp.permission_id
        WHERE u.tenant_id = :tenantId
          AND u.deleted_at IS NULL
          AND p.code = :permissionCode
        """, nativeQuery = true)
    List<User> findByTenantIdAndPermission(
            @Param("tenantId") UUID tenantId,
            @Param("permissionCode") String permissionCode);

    @Query("SELECT COUNT(u) FROM User u WHERE u.tenant.id = :tenantId AND u.chatOnline = true AND u.deletedAt IS NULL")
    long countChatOnlineByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT u.chatOnline FROM User u WHERE u.id = :userId")
    java.util.Optional<Boolean> findChatOnlineById(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE User u SET u.chatOnline = :online WHERE u.id = :userId")
    void updateChatOnline(@Param("userId") UUID userId, @Param("online") boolean online);

    @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId AND u.chatOnline = true AND u.deletedAt IS NULL")
    java.util.List<User> findChatOnlineAgentsByTenantId(@Param("tenantId") UUID tenantId);
}
