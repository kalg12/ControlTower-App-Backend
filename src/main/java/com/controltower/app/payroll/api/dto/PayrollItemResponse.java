package com.controltower.app.payroll.api.dto;

import com.controltower.app.payroll.domain.PayrollItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PayrollItemResponse(
        UUID id,
        UUID employeeId,
        String employeeName,
        String employeeRfc,
        BigDecimal daysWorked,
        BigDecimal overtimeHours,
        BigDecimal grossPay,
        BigDecimal imssEmployee,
        BigDecimal isr,
        BigDecimal infonavit,
        BigDecimal otherDeductions,
        BigDecimal totalDeductions,
        BigDecimal netPay,
        Boolean receiptSent,
        Instant receiptSentAt,
        String notes
) {
    public static PayrollItemResponse from(PayrollItem i) {
        return new PayrollItemResponse(
                i.getId(),
                i.getEmployee().getId(),
                i.getEmployee().getFullName(),
                i.getEmployee().getRfc(),
                i.getDaysWorked(),
                i.getOvertimeHours(),
                i.getGrossPay(),
                i.getImssEmployee(),
                i.getIsr(),
                i.getInfonavit(),
                i.getOtherDeductions(),
                i.getTotalDeductions(),
                i.getNetPay(),
                i.getReceiptSent(),
                i.getReceiptSentAt(),
                i.getNotes()
        );
    }
}
