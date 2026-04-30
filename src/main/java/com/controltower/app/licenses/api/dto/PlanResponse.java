package com.controltower.app.licenses.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class PlanResponse {
    private final UUID id;
    private final String name;
    private final String description;
    private final int maxBranches;
    private final int maxUsers;
    private final BigDecimal priceMonthly;
    private final boolean active;
}
