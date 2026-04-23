package com.controltower.app.payroll.api.dto;

import com.controltower.app.payroll.domain.Employee;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class EmployeeResponse {
    private UUID id;
    private UUID tenantId;
    private String fullName;
    private String rfc;
    private String imssNumber;
    private String curp;
    private String department;
    private String position;
    private Employee.SalaryType salaryType;
    private BigDecimal baseSalary;
    private LocalDate startDate;
    private Employee.EmployeeStatus status;
    private String email;
    private String bankAccount;
    private Instant createdAt;

    public static EmployeeResponse from(Employee e) {
        return EmployeeResponse.builder()
                .id(e.getId())
                .tenantId(e.getTenantId())
                .fullName(e.getFullName())
                .rfc(e.getRfc())
                .imssNumber(e.getImssNumber())
                .curp(e.getCurp())
                .department(e.getDepartment())
                .position(e.getPosition())
                .salaryType(e.getSalaryType())
                .baseSalary(e.getBaseSalary())
                .startDate(e.getStartDate())
                .status(e.getStatus())
                .email(e.getEmail())
                .bankAccount(e.getBankAccount())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
