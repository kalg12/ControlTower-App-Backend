package com.controltower.app.clients.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    private String legalName;
    private String taxId;
    private String country;
    private String notes;
    private String website;
    private String industry;
    private String segment;
    private String leadSource;
    private String phone;
    private String primaryPhone;
    private String primaryEmail;
    private String primaryContactName;
}
