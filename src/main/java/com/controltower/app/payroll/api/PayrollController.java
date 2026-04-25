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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
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
        return ResponseEntity.ok(PageResponse.from(result));
    }

    @Operation(summary = "Create employee")
    @PostMapping("/employees")
    @PreAuthorize("hasAuthority('payroll:write')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> createEmployee(@Valid @RequestBody EmployeeRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(payrollService.createEmployee(req)));
    }

    @Operation(summary = "Update employee")
    @PutMapping("/employees/{id}")
    @PreAuthorize("hasAuthority('payroll:write')")
    public ResponseEntity<ApiResponse<EmployeeResponse>> updateEmployee(
            @PathVariable UUID id, @Valid @RequestBody EmployeeRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.updateEmployee(id, req)));
    }

    @Operation(summary = "Terminate (soft-delete) employee")
    @DeleteMapping("/employees/{id}")
    @PreAuthorize("hasAuthority('payroll:write')")
    public ResponseEntity<ApiResponse<Void>> terminateEmployee(@PathVariable UUID id) {
        payrollService.terminateEmployee(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ── Periods ────────────────────────────────────────────────────

    @Operation(summary = "List payroll periods")
    @GetMapping("/periods")
    @PreAuthorize("hasAuthority('payroll:read')")
    public ResponseEntity<PageResponse<PayrollPeriodResponse>> listPeriods(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "24") int size) {
        return ResponseEntity.ok(PageResponse.from(payrollService.listPeriods(PageRequest.of(page, size))));
    }

    @Operation(summary = "Get payroll period with items")
    @GetMapping("/periods/{id}")
    @PreAuthorize("hasAuthority('payroll:read')")
    public ResponseEntity<ApiResponse<PayrollPeriodResponse>> getPeriod(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getPeriod(id)));
    }

    @Operation(summary = "Create payroll period")
    @PostMapping("/periods")
    @PreAuthorize("hasAuthority('payroll:write')")
    public ResponseEntity<ApiResponse<PayrollPeriodResponse>> createPeriod(@Valid @RequestBody PayrollPeriodRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(payrollService.createPeriod(req)));
    }

    @Operation(summary = "Process payroll period (recalculate all items)")
    @PostMapping("/periods/{id}/process")
    @PreAuthorize("hasAuthority('payroll:write')")
    public ResponseEntity<ApiResponse<PayrollPeriodResponse>> processPeriod(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.processPeriod(id)));
    }

    @Operation(summary = "Close payroll period (mark as PAID)")
    @PostMapping("/periods/{id}/close")
    @PreAuthorize("hasAuthority('payroll:close')")
    public ResponseEntity<ApiResponse<PayrollPeriodResponse>> closePeriod(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.closePeriod(id)));
    }

    @Operation(summary = "Update payroll item (override days/overtime/other)")
    @PutMapping("/periods/{periodId}/items/{itemId}")
    @PreAuthorize("hasAuthority('payroll:write')")
    public ResponseEntity<ApiResponse<PayrollItemResponse>> updateItem(
            @PathVariable UUID periodId,
            @PathVariable UUID itemId,
            @Valid @RequestBody PayrollItemUpdateRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.updateItem(periodId, itemId, req)));
    }

    @Operation(summary = "Send payroll receipt to employee")
    @PostMapping("/periods/{periodId}/items/{itemId}/send-receipt")
    @PreAuthorize("hasAuthority('payroll:write')")
    public ResponseEntity<ApiResponse<PayrollItemResponse>> sendReceipt(
            @PathVariable UUID periodId,
            @PathVariable UUID itemId) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.sendReceipt(periodId, itemId)));
    }

    @Operation(summary = "Download payroll period as CSV")
    @GetMapping("/periods/{id}/export.csv")
    @PreAuthorize("hasAuthority('payroll:read')")
    public ResponseEntity<byte[]> exportCsv(@PathVariable UUID id) {
        PayrollPeriodResponse period = payrollService.getPeriod(id);
        StringBuilder sb = new StringBuilder();
        sb.append("Empleado,RFC,Dias trabajados,Horas extra,Sueldo bruto,IMSS empleado,ISR,INFONAVIT,Otras deducciones,Total deducciones,Neto\n");
        for (PayrollItemResponse item : period.getItems()) {
            sb.append(csv(item.employeeName())).append(',')
              .append(csv(item.employeeRfc())).append(',')
              .append(item.daysWorked()).append(',')
              .append(item.overtimeHours()).append(',')
              .append(item.grossPay()).append(',')
              .append(item.imssEmployee()).append(',')
              .append(item.isr()).append(',')
              .append(item.infonavit()).append(',')
              .append(item.otherDeductions()).append(',')
              .append(item.totalDeductions()).append(',')
              .append(item.netPay()).append('\n');
        }
        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
        String filename = "nomina-" + period.getYear() + "-P" + period.getPeriodNumber() + ".csv";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(bytes);
    }

    @Operation(summary = "Download CFDI XML stub for payroll period (SAT)")
    @GetMapping("/periods/{id}/cfdi.xml")
    @PreAuthorize("hasAuthority('payroll:read')")
    public ResponseEntity<byte[]> exportCfdi(@PathVariable UUID id) {
        PayrollPeriodResponse period = payrollService.getPeriod(id);
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<!-- CFDI de Nómina — Stub generado por Control Tower. Requiere certificación SAT. -->\n");
        xml.append("<cfdi:Comprobante xmlns:cfdi=\"http://www.sat.gob.mx/cfd/4\"\n");
        xml.append("  xmlns:nomina12=\"http://www.sat.gob.mx/nomina12\"\n");
        xml.append("  Version=\"4.0\" TipoDeComprobante=\"N\"\n");
        xml.append("  Fecha=\"").append(LocalDate.now()).append("T00:00:00\"\n");
        xml.append("  SubTotal=\"").append(period.getTotalGross()).append("\"\n");
        xml.append("  Total=\"").append(period.getTotalNet()).append("\"\n");
        xml.append("  Moneda=\"MXN\" LugarExpedicion=\"00000\">\n");
        xml.append("  <cfdi:Complemento>\n");
        xml.append("    <nomina12:Nomina Version=\"1.2\"\n");
        xml.append("      TipoNomina=\"O\"\n");
        xml.append("      FechaPago=\"").append(period.getEndDate()).append("\"\n");
        xml.append("      FechaInicialPago=\"").append(period.getStartDate()).append("\"\n");
        xml.append("      FechaFinalPago=\"").append(period.getEndDate()).append("\"\n");
        xml.append("      NumDiasPagados=\"").append(period.getPeriodType() == com.controltower.app.payroll.domain.PayrollPeriod.PeriodType.QUINCENAL ? 7 : 15).append("\"\n");
        xml.append("      TotalPercepciones=\"").append(period.getTotalGross()).append("\"\n");
        xml.append("      TotalDeducciones=\"").append(period.getTotalDeductions()).append("\"\n");
        xml.append("      TotalOtrosPagos=\"0.00\">\n");
        xml.append("      <nomina12:Receptor>\n");
        xml.append("        <!-- Datos del receptor se generan por empleado en CFDI individual -->\n");
        xml.append("      </nomina12:Receptor>\n");
        for (PayrollItemResponse item : period.getItems()) {
            xml.append("      <!-- Empleado: ").append(item.employeeName())
               .append(" RFC: ").append(item.employeeRfc())
               .append(" Neto: ").append(item.netPay()).append(" -->\n");
        }
        xml.append("    </nomina12:Nomina>\n");
        xml.append("  </cfdi:Complemento>\n");
        xml.append("</cfdi:Comprobante>\n");
        byte[] bytes = xml.toString().getBytes(StandardCharsets.UTF_8);
        String filename = "cfdi-nomina-" + period.getYear() + "-P" + period.getPeriodNumber() + ".xml";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(bytes);
    }

    private static String csv(String value) {
        if (value == null) return "";
        return value.contains(",") || value.contains("\"") || value.contains("\n")
                ? "\"" + value.replace("\"", "\"\"") + "\""
                : value;
    }
}
