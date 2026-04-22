package com.controltower.app.persons.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PersonResponse {
    private UUID id;
    private UUID tenantId;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private String whatsapp;
    private LocalDate birthDate;
    private String notes;
    private String leadSource;
    private String status;
    private UUID assignedToId;
    private String assignedToName;
    private UUID clientId;
    private String clientName;
    private String address;
    private List<String> tags;
    private Instant createdAt;
    private Instant updatedAt;
}
