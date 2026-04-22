package com.controltower.app.persons.domain;

import com.controltower.app.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "persons")
@Getter
@Setter
public class Person extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "email")
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "whatsapp", length = 50)
    private String whatsapp;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "lead_source", length = 50)
    private String leadSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PersonStatus status = PersonStatus.PROSPECT;

    @Column(name = "assigned_to_id")
    private UUID assignedToId;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "address", length = 500)
    private String address;

    @Array(length = 30)
    @Column(name = "tags", columnDefinition = "TEXT[]")
    private String[] tags = new String[0];

    public String getFullName() {
        if (lastName == null || lastName.isBlank()) return firstName;
        return firstName + " " + lastName;
    }

    public enum PersonStatus {
        PROSPECT, ACTIVE, INACTIVE, CONVERTED
    }
}
