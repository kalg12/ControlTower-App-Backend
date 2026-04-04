package com.controltower.app.identity.api.dto;

import com.controltower.app.identity.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.UUID;

/**
 * Partial update: only non-null fields are applied. {@code roleIds}:
 * {@code null} leaves roles unchanged; empty set clears all roles.
 */
@Getter
@Setter
public class UpdateUserRequest {

    private String fullName;

    @Email(message = "Must be a valid email address")
    private String email;

    private User.UserStatus status;

    private Set<UUID> roleIds;

    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
