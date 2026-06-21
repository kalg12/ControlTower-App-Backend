package com.controltower.app.email.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(
    @NotBlank @Size(max = 100) String name,
    String description,
    @Size(max = 7) String color,
    @Size(max = 50) String icon,
    Integer slaHours
) {}
