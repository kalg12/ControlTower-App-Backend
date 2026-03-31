package com.controltower.app.licenses.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ActivateLicenseRequest {

    @NotNull(message = "clientId is required")
    private UUID clientId;

    @NotNull(message = "planId is required")
    private UUID planId;

    /** Override default 14-day trial. Null uses the default. */
    private Integer trialDays;

    private String stripeSubscriptionId;
}
