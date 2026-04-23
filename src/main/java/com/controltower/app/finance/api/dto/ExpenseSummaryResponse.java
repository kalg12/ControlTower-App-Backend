package com.controltower.app.finance.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ExpenseSummaryResponse(
        Instant from,
        Instant to,
        BigDecimal grandTotal,
        List<CategoryBreakdown> byCategory,
        List<MonthlyBreakdown> byMonth
) {

    public record CategoryBreakdown(
            String category,
            BigDecimal total,
            long count,
            double percentage
    ) {}

    public record MonthlyBreakdown(
            String month,
            BigDecimal total,
            Map<String, BigDecimal> byCategory
    ) {}
}
