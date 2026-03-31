package com.controltower.app.identity.application;

import com.controltower.app.identity.domain.*;
import com.controltower.app.shared.exception.ControlTowerException;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public Page<Role> listRoles(Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        return roleRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable);
    }

    @Transactional
    public Role createRole(String name, String code, String description) {
        UUID tenantId = TenantContext.getTenantId();
        if (roleRepository.findByCodeAndTenantId(code, tenantId).isPresent()) {
            throw new ControlTowerException("Role code already exists: " + code, HttpStatus.CONFLICT);
        }
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));
        Role role = new Role();
        role.setTenant(tenant);
        role.setName(name);
        role.setCode(code);
        role.setDescription(description);
        role.setSystem(false);
        return roleRepository.save(role);
    }

    @Transactional
    public void deleteRole(UUID roleId) {
        UUID tenantId = TenantContext.getTenantId();
        Role role = roleRepository.findByIdAndTenantIdAndDeletedAtIsNull(roleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));
        if (role.isSystem()) {
            throw new ControlTowerException("Cannot delete a system role", HttpStatus.FORBIDDEN);
        }
        role.softDelete();
        roleRepository.save(role);
    }

    @Transactional
    public Role addPermission(UUID roleId, UUID permissionId) {
        UUID tenantId = TenantContext.getTenantId();
        Role role = roleRepository.findByIdAndTenantIdAndDeletedAtIsNull(roleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", permissionId));
        role.getPermissions().add(permission);
        return roleRepository.save(role);
    }

    @Transactional
    public Role removePermission(UUID roleId, UUID permissionId) {
        UUID tenantId = TenantContext.getTenantId();
        Role role = roleRepository.findByIdAndTenantIdAndDeletedAtIsNull(roleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", permissionId));
        role.getPermissions().remove(permission);
        return roleRepository.save(role);
    }

    @Transactional
    public void assignRoleToUser(UUID userId, UUID roleId) {
        UUID tenantId = TenantContext.getTenantId();
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (!user.getTenant().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("User", userId);
        }
        Role role = roleRepository.findByIdAndTenantIdAndDeletedAtIsNull(roleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));
        user.getRoles().add(role);
        userRepository.save(user);
    }

    @Transactional
    public void removeRoleFromUser(UUID userId, UUID roleId) {
        UUID tenantId = TenantContext.getTenantId();
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        if (!user.getTenant().getId().equals(tenantId)) {
            throw new ResourceNotFoundException("User", userId);
        }
        Role role = roleRepository.findByIdAndTenantIdAndDeletedAtIsNull(roleId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", roleId));
        user.getRoles().remove(role);
        userRepository.save(user);
    }
}
