package com.controltower.app.payroll.domain;

import com.controltower.app.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "employees")
@Getter
@Setter
public class Employee extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "rfc", nullable = false, length = 13)
    private String rfc;

    @Column(name = "imss_number", length = 11)
    private String imssNumber;

    @Column(name = "curp", length = 18)
    private String curp;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "position", length = 100)
    private String position;

    @Enumerated(EnumType.STRING)
    @Column(name = "salary_type", nullable = false, length = 20)
    private SalaryType salaryType;

    @Column(name = "base_salary", nullable = false, precision = 14, scale = 2)
    private BigDecimal baseSalary;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "bank_account", length = 30)
    private String bankAccount;

    public enum SalaryType { MONTHLY, BIWEEKLY }

    public enum EmployeeStatus { ACTIVE, INACTIVE, TERMINATED }
}
