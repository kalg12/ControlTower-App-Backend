package com.controltower.app.identity.api.dto;

import com.controltower.app.identity.domain.Permission;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class PermissionResponse {

    private final UUID id;
    private final String code;
    private final String description;
    private final String module;

    public static PermissionResponse from(Permission permission) {
        return PermissionResponse.builder()
                .id(permission.getId())
                .code(permission.getCode())
                .description(permission.getDescription())
                .module(permission.getModule())
                .build();
    }
}
