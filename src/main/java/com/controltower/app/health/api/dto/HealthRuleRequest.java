package com.controltower.app.health.api.dto;

import com.controltower.app.health.domain.HealthIncident;
import com.controltower.app.health.domain.HealthRule;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class HealthRuleRequest {

    /** If null, the rule applies to all branches in the tenant. */
    private UUID branchId;

    @NotNull(message = "Rule type is required")
    private HealthRule.RuleType ruleType;

    @Min(value = 1, message = "Threshold must be at least 1")
    private Integer thresholdValue;

    private HealthIncident.Severity severity = HealthIncident.Severity.MEDIUM;
    private String alertChannel;
}
