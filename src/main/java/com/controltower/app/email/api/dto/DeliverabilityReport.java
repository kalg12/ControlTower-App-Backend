package com.controltower.app.email.api.dto;

import java.util.List;

public record DeliverabilityReport(
        List<Check> checks,
        int score,
        String verdict
) {
    public record Check(
            String name,
            boolean passed,
            String message,
            String action
    ) {}

    public static String verdict(int score) {
        if (score >= 80) return "GOOD";
        if (score >= 50) return "FAIR";
        return "POOR";
    }
}
