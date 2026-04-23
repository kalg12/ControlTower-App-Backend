package com.controltower.app.payroll.application;

import com.controltower.app.audit.application.AuditService;
import com.controltower.app.audit.domain.AuditAction;
import com.controltower.app.payroll.api.dto.*;
import com.controltower.app.payroll.domain.*;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollService {

    private final EmployeeRepository employeeRepository;
    private final PayrollPeriodRepository periodRepository;
    private final PayrollItemRepository itemRepository;
    private final AuditService auditService;
    private final com.controltower.app.shared.infrastructure.EmailService emailService;

    // ──────────────────────────────────────────────────────────────
    // ISR 2024 SAT monthly bracket table (11 tramos)
    // Each row: { lowerLimit, fixedFee, rate }
    // ──────────────────────────────────────────────────────────────
    private static final double[][] ISR_MONTHLY = {
        {     0.01,    0.00,  0.0192 },
        {   746.05,   14.32,  0.0640 },
        {  6332.06,  371.83,  0.1088 },
        { 11128.02,  893.63,  0.1600 },
        { 12935.83, 1182.88,  0.1792 },
        { 15487.72, 1640.18,  0.2136 },
        { 30992.01, 4952.03,  0.2352 },
        { 32736.84, 5353.51,  0.3000 },
        { 62500.01, 14277.01, 0.3200 },
        {104166.68, 27460.93, 0.3400 },
        {166666.68, 48592.09, 0.3500 },
    };

    // ──────────────────────────────────────────────────────────────
    // Employees
    // ──────────────────────────────────────────────────────────────

    public Page<EmployeeResponse> listEmployees(Employee.EmployeeStatus status, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        return employeeRepository.findByTenantId(tenantId, status, pageable)
                .map(EmployeeResponse::from);
    }

    @Transactional
    public EmployeeResponse createEmployee(EmployeeRequest req) {
        UUID tenantId = TenantContext.getTenantId();
        if (employeeRepository.existsByTenantIdAndRfcAndDeletedAtIsNull(tenantId, req.rfc())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "RFC already exists for this tenant");
        }
        Employee e = new Employee();
        mapRequest(e, req, tenantId);
        employeeRepository.save(e);
        auditService.log(AuditAction.EMPLOYEE_CREATED, tenantId, currentUserId(), "Employee", e.getId().toString());
        return EmployeeResponse.from(e);
    }

    @Transactional
    public EmployeeResponse updateEmployee(UUID id, EmployeeRequest req) {
        UUID tenantId = TenantContext.getTenantId();
        Employee e = findEmployee(id, tenantId);
        if (e.getStatus() == Employee.EmployeeStatus.TERMINATED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Cannot edit a terminated employee");
        }
        if (employeeRepository.existsByTenantIdAndRfcAndIdNotAndDeletedAtIsNull(tenantId, req.rfc(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "RFC already exists for another employee");
        }
        mapRequest(e, req, tenantId);
        employeeRepository.save(e);
        auditService.log(AuditAction.EMPLOYEE_UPDATED, tenantId, currentUserId(), "Employee", id.toString());
        return EmployeeResponse.from(e);
    }

    @Transactional
    public void terminateEmployee(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        Employee e = findEmployee(id, tenantId);
        e.setStatus(Employee.EmployeeStatus.TERMINATED);
        e.softDelete();
        employeeRepository.save(e);
        auditService.log(AuditAction.EMPLOYEE_TERMINATED, tenantId, currentUserId(), "Employee", id.toString());
    }

    // ──────────────────────────────────────────────────────────────
    // Payroll Periods
    // ──────────────────────────────────────────────────────────────

    public Page<PayrollPeriodResponse> listPeriods(Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        return periodRepository.findByTenantId(tenantId, pageable)
                .map(PayrollPeriodResponse::fromNoItems);
    }

    public PayrollPeriodResponse getPeriod(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        PayrollPeriod period = findPeriod(id, tenantId);
        List<PayrollItemResponse> items = itemRepository.findByPeriodId(id)
                .stream().map(PayrollItemResponse::from).toList();
        return PayrollPeriodResponse.from(period, items);
    }

    @Transactional
    public PayrollPeriodResponse createPeriod(PayrollPeriodRequest req) {
        UUID tenantId = TenantContext.getTenantId();
        PayrollPeriod period = new PayrollPeriod();
        period.setTenantId(tenantId);
        period.setYear(req.year());
        period.setPeriodNumber(req.periodNumber());
        period.setPeriodType(req.periodType());
        period.setStartDate(req.startDate());
        period.setEndDate(req.endDate());
        period.setNotes(req.notes());
        period.setStatus(PayrollPeriod.PeriodStatus.DRAFT);
        periodRepository.save(period);

        // Generate items for all active employees
        List<Employee> active = employeeRepository.findByTenantId(tenantId, Employee.EmployeeStatus.ACTIVE, Pageable.unpaged()).getContent();
        int daysInPeriod = req.periodType() == PayrollPeriod.PeriodType.QUINCENAL ? 15 : 30;
        for (Employee emp : active) {
            PayrollItem item = buildItem(period, emp, BigDecimal.valueOf(daysInPeriod), BigDecimal.ZERO, BigDecimal.ZERO);
            itemRepository.save(item);
            period.getItems().add(item);
        }
        period.recalculateTotals();
        periodRepository.save(period);

        auditService.log(AuditAction.PAYROLL_PERIOD_CREATED, tenantId, currentUserId(), "PayrollPeriod", period.getId().toString());
        List<PayrollItemResponse> itemResponses = period.getItems().stream().map(PayrollItemResponse::from).toList();
        return PayrollPeriodResponse.from(period, itemResponses);
    }

    @Transactional
    public PayrollPeriodResponse processPeriod(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        PayrollPeriod period = findPeriod(id, tenantId);
        if (period.getStatus() != PayrollPeriod.PeriodStatus.DRAFT) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Only DRAFT periods can be processed");
        }
        int daysInPeriod = period.getPeriodType() == PayrollPeriod.PeriodType.QUINCENAL ? 15 : 30;
        List<PayrollItem> items = itemRepository.findByPeriodId(id);
        for (PayrollItem item : items) {
            recalcItem(item, item.getDaysWorked(), item.getOvertimeHours(), item.getOtherDeductions(), period.getPeriodType());
            itemRepository.save(item);
        }
        period.setStatus(PayrollPeriod.PeriodStatus.PROCESSED);
        period.setItems(items);
        period.recalculateTotals();
        periodRepository.save(period);
        auditService.log(AuditAction.PAYROLL_PERIOD_PROCESSED, tenantId, currentUserId(), "PayrollPeriod", id.toString());
        List<PayrollItemResponse> responses = items.stream().map(PayrollItemResponse::from).toList();
        return PayrollPeriodResponse.from(period, responses);
    }

    @Transactional
    public PayrollPeriodResponse closePeriod(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        PayrollPeriod period = findPeriod(id, tenantId);
        if (period.getStatus() != PayrollPeriod.PeriodStatus.PROCESSED) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Only PROCESSED periods can be closed");
        }
        period.setStatus(PayrollPeriod.PeriodStatus.PAID);
        periodRepository.save(period);
        auditService.log(AuditAction.PAYROLL_PERIOD_PAID, tenantId, currentUserId(), "PayrollPeriod", id.toString());
        List<PayrollItemResponse> responses = itemRepository.findByPeriodId(id).stream().map(PayrollItemResponse::from).toList();
        return PayrollPeriodResponse.from(period, responses);
    }

    @Transactional
    public PayrollItemResponse updateItem(UUID periodId, UUID itemId, PayrollItemUpdateRequest req) {
        UUID tenantId = TenantContext.getTenantId();
        PayrollPeriod period = findPeriod(periodId, tenantId);
        if (period.getStatus() == PayrollPeriod.PeriodStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Cannot edit items of a PAID period");
        }
        PayrollItem item = itemRepository.findByIdAndTenantId(itemId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        recalcItem(item, req.daysWorked(), req.overtimeHours(), req.otherDeductions(), period.getPeriodType());
        if (req.notes() != null) item.setNotes(req.notes());
        itemRepository.save(item);
        List<PayrollItem> allItems = itemRepository.findByPeriodId(periodId);
        period.setItems(allItems);
        period.recalculateTotals();
        periodRepository.save(period);
        return PayrollItemResponse.from(item);
    }

    @Transactional
    public PayrollItemResponse sendReceipt(UUID periodId, UUID itemId) {
        UUID tenantId = TenantContext.getTenantId();
        PayrollPeriod period = findPeriod(periodId, tenantId);
        PayrollItem item = itemRepository.findByIdAndTenantId(itemId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Employee emp = item.getEmployee();
        if (emp.getEmail() == null || emp.getEmail().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Employee has no email configured");
        }
        String periodLabel = period.getPeriodType().name() + " " + period.getPeriodNumber() + "/" + period.getYear();
        emailService.sendReciboNomina(
                emp.getEmail(), emp.getFullName(), periodLabel,
                item.getGrossPay().toPlainString(),
                item.getImssEmployee().toPlainString(),
                item.getIsr().toPlainString(),
                item.getInfonavit().toPlainString(),
                item.getOtherDeductions().toPlainString(),
                item.getNetPay().toPlainString(),
                "MXN");
        item.setReceiptSent(true);
        item.setReceiptSentAt(java.time.Instant.now());
        itemRepository.save(item);
        auditService.log(AuditAction.PAYROLL_RECEIPT_SENT, tenantId, currentUserId(), "PayrollItem", itemId.toString());
        return PayrollItemResponse.from(item);
    }

    // ──────────────────────────────────────────────────────────────
    // Calculations
    // ──────────────────────────────────────────────────────────────

    private PayrollItem buildItem(PayrollPeriod period, Employee emp, BigDecimal daysWorked, BigDecimal overtimeHours, BigDecimal otherDeductions) {
        PayrollItem item = new PayrollItem();
        item.setTenantId(period.getTenantId());
        item.setPeriod(period);
        item.setEmployee(emp);
        recalcItem(item, daysWorked, overtimeHours, otherDeductions, period.getPeriodType());
        return item;
    }

    void recalcItem(PayrollItem item, BigDecimal daysWorked, BigDecimal overtimeHours, BigDecimal otherDeductions, PayrollPeriod.PeriodType periodType) {
        BigDecimal baseSalary = item.getEmployee().getBaseSalary();
        int divisor = periodType == PayrollPeriod.PeriodType.QUINCENAL ? 15 : 30;
        BigDecimal dailySalary = baseSalary.divide(BigDecimal.valueOf(divisor), 6, RoundingMode.HALF_UP);

        BigDecimal overtimeBonus = dailySalary.divide(BigDecimal.valueOf(8), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(1.5)).multiply(overtimeHours);
        BigDecimal grossPay = dailySalary.multiply(daysWorked).add(overtimeBonus).setScale(2, RoundingMode.HALF_UP);

        BigDecimal imss = baseSalary.multiply(new BigDecimal("0.0165")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal infonavit = baseSalary.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal isr = calculateIsr(grossPay, periodType);
        BigDecimal totalDed = imss.add(isr).add(infonavit).add(otherDeductions).setScale(2, RoundingMode.HALF_UP);
        BigDecimal netPay = grossPay.subtract(totalDed).setScale(2, RoundingMode.HALF_UP);

        item.setDaysWorked(daysWorked);
        item.setOvertimeHours(overtimeHours);
        item.setGrossPay(grossPay);
        item.setImssEmployee(imss);
        item.setIsr(isr);
        item.setInfonavit(infonavit);
        item.setOtherDeductions(otherDeductions);
        item.setTotalDeductions(totalDed);
        item.setNetPay(netPay);
    }

    BigDecimal calculateIsr(BigDecimal grossPay, PayrollPeriod.PeriodType periodType) {
        // Annualize, apply SAT 2024 monthly bracket, de-annualize
        int periods = periodType == PayrollPeriod.PeriodType.QUINCENAL ? 24 : 12;
        double annual = grossPay.doubleValue() * periods;
        double monthlyBase = annual / 12.0;

        double[] bracket = ISR_MONTHLY[0];
        for (double[] row : ISR_MONTHLY) {
            if (monthlyBase >= row[0]) bracket = row;
            else break;
        }
        double monthlyIsr = bracket[1] + (monthlyBase - bracket[0]) * bracket[2];
        double annualIsr = monthlyIsr * 12;
        double periodIsr = annualIsr / periods;
        return BigDecimal.valueOf(periodIsr).setScale(2, RoundingMode.HALF_UP).max(BigDecimal.ZERO);
    }

    // ──────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────

    private Employee findEmployee(UUID id, UUID tenantId) {
        return employeeRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
    }

    private PayrollPeriod findPeriod(UUID id, UUID tenantId) {
        return periodRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payroll period not found"));
    }

    private void mapRequest(Employee e, EmployeeRequest req, UUID tenantId) {
        e.setTenantId(tenantId);
        e.setFullName(req.fullName());
        e.setRfc(req.rfc());
        e.setImssNumber(req.imssNumber());
        e.setCurp(req.curp());
        e.setDepartment(req.department());
        e.setPosition(req.position());
        e.setSalaryType(req.salaryType());
        e.setBaseSalary(req.baseSalary());
        e.setStartDate(req.startDate());
        e.setEmail(req.email());
        e.setBankAccount(req.bankAccount());
        if (e.getStatus() == null) e.setStatus(Employee.EmployeeStatus.ACTIVE);
    }

    private UUID currentUserId() {
        try {
            return UUID.fromString(SecurityContextHolder.getContext().getAuthentication().getName());
        } catch (Exception ex) {
            return null;
        }
    }
}
