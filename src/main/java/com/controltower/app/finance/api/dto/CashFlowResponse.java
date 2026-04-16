package com.controltower.app.finance.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record CashFlowResponse(
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal netFlow,
        List<MonthlyEntry> byMonth
) {
    public record MonthlyEntry(
            String month,   // e.g. "2026-01"
            BigDecimal income,
            BigDecimal expenses,
            BigDecimal net
    ) {}
}
