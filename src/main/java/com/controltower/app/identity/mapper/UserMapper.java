package com.controltower.app.identity.mapper;

import com.controltower.app.identity.api.dto.UserResponse;
import com.controltower.app.identity.domain.Role;
import com.controltower.app.identity.domain.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "tenantId",    source = "tenant.id")
    @Mapping(target = "roles",       source = "roles",      qualifiedByName = "rolesToCodes")
    @Mapping(target = "permissions", source = "user",       qualifiedByName = "toPermissions")
    @Mapping(target = "superAdmin",  source = "superAdmin")
    UserResponse toResponse(User user);

    @Named("rolesToCodes")
    static Set<String> rolesToCodes(Set<Role> roles) {
        return roles.stream().map(Role::getCode).collect(Collectors.toSet());
    }

    @Named("toPermissions")
    static Set<String> toPermissions(User user) {
        return user.getAllPermissions();
    }
}
