package com.controltower.app.email.api;

import com.controltower.app.email.api.dto.DepartmentRequest;
import com.controltower.app.email.api.dto.DepartmentResponse;
import com.controltower.app.email.domain.Department;
import com.controltower.app.email.domain.DepartmentRepository;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.tenancy.domain.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Email — Departments", description = "Manage support departments for email routing")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/email/departments")
@RequiredArgsConstructor
public class EmailDepartmentController {

    private final DepartmentRepository departmentRepo;

    @GetMapping
    @PreAuthorize("hasAuthority('email:read')")
    @Operation(summary = "List all departments for the current tenant")
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> list() {
        UUID tenantId = TenantContext.getTenantId();
        List<DepartmentResponse> data = departmentRepo.findByTenantIdAndActiveTrue(tenantId)
            .stream().map(DepartmentResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('email:write')")
    @Operation(summary = "Create a new department")
    public ResponseEntity<ApiResponse<DepartmentResponse>> create(@Valid @RequestBody DepartmentRequest req) {
        UUID tenantId = TenantContext.getTenantId();
        Department dept = new Department();
        dept.setTenantId(tenantId);
        dept.setName(req.name());
        dept.setDescription(req.description());
        dept.setColor(req.color());
        dept.setIcon(req.icon());
        dept.setSlaHours(req.slaHours() != null ? req.slaHours() : 24);
        departmentRepo.save(dept);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Department created", DepartmentResponse.from(dept)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('email:write')")
    @Operation(summary = "Update a department")
    public ResponseEntity<ApiResponse<DepartmentResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody DepartmentRequest req) {
        UUID tenantId = TenantContext.getTenantId();
        Department dept = departmentRepo.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        dept.setName(req.name());
        dept.setDescription(req.description());
        dept.setColor(req.color());
        dept.setIcon(req.icon());
        if (req.slaHours() != null) dept.setSlaHours(req.slaHours());
        departmentRepo.save(dept);
        return ResponseEntity.ok(ApiResponse.ok("Department updated", DepartmentResponse.from(dept)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('email:manage')")
    @Operation(summary = "Soft-delete a department")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        Department dept = departmentRepo.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Department not found"));
        dept.setActive(false);
        departmentRepo.save(dept);
        return ResponseEntity.ok(ApiResponse.ok("Department deactivated"));
    }
}
