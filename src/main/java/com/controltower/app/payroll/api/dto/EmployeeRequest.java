package com.controltower.app.payroll.api.dto;

import com.controltower.app.payroll.domain.Employee;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeRequest(
        @NotBlank @Size(max = 200) String fullName,
        @NotBlank @Pattern(regexp = "^[A-Z&Ñ]{4}\\d{6}[A-Z\\d]{3}$") String rfc,
        @Size(max = 11) String imssNumber,
        @Size(max = 18) String curp,
        @Size(max = 100) String department,
        @Size(max = 100) String position,
        @NotNull Employee.SalaryType salaryType,
        @NotNull @DecimalMin("0.01") BigDecimal baseSalary,
        @NotNull LocalDate startDate,
        @Email @Size(max = 255) String email,
        @Size(max = 30) String bankAccount
) {}
