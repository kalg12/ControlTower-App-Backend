package com.controltower.app.identity.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class ReplaceRolePermissionsRequest {

    /** New permission set for the role (may be empty). */
    @NotNull(message = "permissionIds is required (use empty set to clear)")
    private Set<UUID> permissionIds;
}
