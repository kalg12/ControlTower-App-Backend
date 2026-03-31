package com.controltower.app.identity.api;

import com.controltower.app.identity.api.dto.CreateRoleRequest;
import com.controltower.app.identity.api.dto.PermissionResponse;
import com.controltower.app.identity.api.dto.RoleResponse;
import com.controltower.app.identity.application.RoleService;
import com.controltower.app.identity.domain.PermissionRepository;
import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Tag(name = "Roles & Permissions", description = "Role and permission management")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final PermissionRepository permissionRepository;

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<ApiResponse<PageResponse<RoleResponse>>> listRoles(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PageResponse<RoleResponse> result = PageResponse.from(
                roleService.listRoles(pageable).map(RoleResponse::from));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('user:write')")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @Valid @RequestBody CreateRoleRequest request) {
        RoleResponse role = RoleResponse.from(
                roleService.createRole(request.getName(), request.getCode(), request.getDescription()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Role created", role));
    }

    @DeleteMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('user:write')")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable UUID id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.ok("Role deleted"));
    }

    @PostMapping("/roles/{id}/permissions/{permissionId}")
    @PreAuthorize("hasAuthority('user:write')")
    public ResponseEntity<ApiResponse<RoleResponse>> addPermission(
            @PathVariable UUID id,
            @PathVariable UUID permissionId) {
        RoleResponse role = RoleResponse.from(roleService.addPermission(id, permissionId));
        return ResponseEntity.ok(ApiResponse.ok("Permission added", role));
    }

    @DeleteMapping("/roles/{id}/permissions/{permissionId}")
    @PreAuthorize("hasAuthority('user:write')")
    public ResponseEntity<ApiResponse<RoleResponse>> removePermission(
            @PathVariable UUID id,
            @PathVariable UUID permissionId) {
        RoleResponse role = RoleResponse.from(roleService.removePermission(id, permissionId));
        return ResponseEntity.ok(ApiResponse.ok("Permission removed", role));
    }

    @PostMapping("/users/{id}/roles/{roleId}")
    @PreAuthorize("hasAuthority('user:write')")
    public ResponseEntity<ApiResponse<Void>> assignRoleToUser(
            @PathVariable UUID id,
            @PathVariable UUID roleId) {
        roleService.assignRoleToUser(id, roleId);
        return ResponseEntity.ok(ApiResponse.ok("Role assigned to user"));
    }

    @DeleteMapping("/users/{id}/roles/{roleId}")
    @PreAuthorize("hasAuthority('user:write')")
    public ResponseEntity<ApiResponse<Void>> removeRoleFromUser(
            @PathVariable UUID id,
            @PathVariable UUID roleId) {
        roleService.removeRoleFromUser(id, roleId);
        return ResponseEntity.ok(ApiResponse.ok("Role removed from user"));
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> listPermissions() {
        List<PermissionResponse> permissions = permissionRepository.findAll().stream()
                .map(PermissionResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(permissions));
    }
}
