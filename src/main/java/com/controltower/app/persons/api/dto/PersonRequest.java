package com.controltower.app.persons.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class PersonRequest {

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    private String email;
    private String phone;
    private String whatsapp;
    private LocalDate birthDate;
    private String notes;
    private String leadSource;
    private String status;
    private UUID assignedToId;
    private UUID clientId;
    private String address;
    private List<String> tags;
}
