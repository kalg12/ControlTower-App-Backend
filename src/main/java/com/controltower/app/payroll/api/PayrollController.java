package com.controltower.app.payroll.api;

import com.controltower.app.payroll.api.dto.*;
import com.controltower.app.payroll.application.PayrollService;
import com.controltower.app.payroll.domain.Employee;
import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Payroll", description = "Mexican payroll management (IMSS, ISR, INFONAVIT)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    // ── Employees ──────────────────────────────────────────────────

    @Operation(summary = "List employees")
    @GetMapping("/employees")
    @PreAuthorize("hasAuthority('payroll:read')")
    public ResponseEntity<PageResponse<EmployeeResponse>> listEmployees(
            @RequestParam(required = false) Employee.EmployeeStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var result = payrollService.listEmployees(status, PageRequest.of(page, size, Sort.by("fullName")));
        return ResponseEntity.ok(PageResponse.of(result));
    }

    @Operation(summary = "Create employee")
    @PostMapping("/employees")
    @PreAuthorize("hasAuthority('payroll:write')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> createEmployee(@Valid @RequestBody EmployeeRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(payrollService.createEmployee(req)));
    }

    @Operation(summary = "Update employee")
    @PutMapping("/employees/{id}")
    @PreAuthorize("hasAuthority('payroll:write')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployee(
            @PathVariable UUID id, @Valid @RequestBody EmployeeRequest req) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.updateEmployee(id, req)));
    }

    @Operation(summary = "Terminate (soft-delete) employee")
    @DeleteMapping("/employees/{id}")
    @PreAuthorize("hasAuthority('payroll:write')")
    public ResponseEntity<ApiResponse<Void>> terminateEmployee(@PathVariable UUID id) {
        payrollService.terminateEmployee(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── Periods ────────────────────────────────────────────────────

    @Operation(summary = "List payroll periods")
    @GetMapping("/periods")
    @PreAuthorize("hasAuthority('payroll:read')")
    public ResponseEntity<PageResponse<PayrollPeriodResponse>> listPeriods(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        return ResponseEntity.ok(PageResponse.of(payrollService.listPeriods(PageRequest.of(page, size))));
    }

    @Operation(summary = "Get payroll period with items")
    @GetMapping("/periods/{id}")
    @PreAuthorize("hasAuthority('payroll:read')")
    public ResponseEntity<ApiResponse<PayrollPeriodResponse>> getPeriod(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.getPeriod(id)));
    }

    @Operation(summary = "Create payroll period")
    @PostMapping("/periods")
    @PreAuthorize("hasAuthority('payroll:write')")
    public ResponseEntity<ApiResponse<PayrollPeriodResponse>> createPeriod(@Valid @RequestBody PayrollPeriodRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(payrollService.createPeriod(req)));
    }

    @Operation(summary = "Process payroll period (recalculate all items)")
    @PostMapping("/periods/{id}/process")
    @PreAuthorize("hasAuthority('payroll:write')")
    public ResponseEntity<ApiResponse<PayrollPeriodResponse>> processPeriod(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.processPeriod(id)));
    }

    @Operation(summary = "Close payroll period (mark as PAID)")
    @PostMapping("/periods/{id}/close")
    @PreAuthorize("hasAuthority('payroll:close')")
    public ResponseEntity<ApiResponse<PayrollPeriodResponse>> closePeriod(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.closePeriod(id)));
    }

    @Operation(summary = "Update payroll item (override days/overtime/other)")
    @PutMapping("/periods/{periodId}/items/{itemId}")
    @PreAuthorize("hasAuthority('payroll:write')")
    public ResponseEntity<ApiResponse<PayrollItemResponse>> updateItem(
            @PathVariable UUID periodId,
            @PathVariable UUID itemId,
            @Valid @RequestBody PayrollItemUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.updateItem(periodId, itemId, req)));
    }

    @Operation(summary = "Send payroll receipt to employee")
    @PostMapping("/periods/{periodId}/items/{itemId}/send-receipt")
    @PreAuthorize("hasAuthority('payroll:write')")
    public ResponseEntity<ApiResponse<PayrollItemResponse>> sendReceipt(
            @PathVariable UUID periodId,
            @PathVariable UUID itemId) {
        return ResponseEntity.ok(ApiResponse.success(payrollService.sendReceipt(periodId, itemId)));
    }
}
