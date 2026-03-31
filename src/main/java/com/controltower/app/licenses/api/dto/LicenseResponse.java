package com.controltower.app.licenses.api.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class LicenseResponse {

    private final UUID    id;
    private final UUID    tenantId;
    private final UUID    clientId;
    private final UUID    planId;
    private final String  planName;
    private final String  status;
    private final Instant currentPeriodStart;
    private final Instant currentPeriodEnd;
    private final Instant gracePeriodEnd;
    private final String  stripeSubscriptionId;
}
