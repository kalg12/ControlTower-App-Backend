package com.controltower.app.identity.api;

import com.controltower.app.identity.api.dto.CreateUserRequest;
import com.controltower.app.identity.api.dto.UpdateUserRequest;
import com.controltower.app.identity.api.dto.UserResponse;
import com.controltower.app.identity.application.UserService;
import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.shared.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

/**
 * User management endpoints. Requires user:read / user:write permissions.
 */
@Tag(name = "Users", description = "User management (admin only)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "List users", description = "Returns a paginated list of users belonging to the specified tenant. Requires the 'user:read' permission.")
    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> listUsers(
            @RequestParam UUID tenantId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        PageResponse<UserResponse> result = PageResponse.from(userService.listUsers(tenantId, pageable));
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "Get user by ID", description = "Retrieves the full profile of a single user by their UUID. Requires the 'user:read' permission.")
    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUser(userId)));
    }

    @Operation(summary = "Create user", description = "Creates a new user under the specified tenant with the provided details. Requires the 'user:write' permission.")
    @PostMapping
    @PreAuthorize("hasAuthority('user:write')")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @RequestParam UUID tenantId,
            @Valid @RequestBody CreateUserRequest request) {
        UserResponse created = userService.createUser(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("User created", created));
    }

    @Operation(summary = "Update user", description = "Updates profile fields, status, roles, and optionally password. Omitted fields stay unchanged; send an empty roleIds array to remove all roles. Requires the 'user:write' permission.")
    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('user:write')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request) {
        UserResponse updated = userService.updateUser(userId, request);
        return ResponseEntity.ok(ApiResponse.ok("User updated", updated));
    }

    @Operation(summary = "Delete user", description = "Permanently removes the user with the given UUID. Requires the 'user:write' permission.")
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('user:write')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.ok("User deleted"));
    }
}
