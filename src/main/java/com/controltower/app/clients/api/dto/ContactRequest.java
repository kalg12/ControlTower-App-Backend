package com.controltower.app.clients.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContactRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 255)
    private String fullName;

    @Email(message = "Invalid email address")
    @Size(max = 255)
    private String email;

    @Size(max = 50)
    private String phone;

    /** OWNER | TECHNICAL | BILLING | SUPPORT */
    private String role;

    private boolean primary;

    private String notes;
}
