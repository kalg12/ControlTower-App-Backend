package com.controltower.app.finance.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record PnlReportResponse(
        Instant from,
        Instant to,
        BigDecimal totalIncome,
        BigDecimal totalExpenses,
        BigDecimal netProfit,
        double marginPct,
        List<MonthlyPnl> byMonth,
        Map<String, BigDecimal> incomeByCategory,
        Map<String, BigDecimal> expensesByCategory
) {
    public record MonthlyPnl(
            String month,
            BigDecimal income,
            BigDecimal expenses,
            BigDecimal purchases,
            BigDecimal net
    ) {}
}
