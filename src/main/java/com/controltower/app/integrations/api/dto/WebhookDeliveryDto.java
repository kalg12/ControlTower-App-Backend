package com.controltower.app.integrations.api.dto;

import com.controltower.app.integrations.domain.WebhookDelivery;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class WebhookDeliveryDto {
    private UUID   id;
    private String url;
    private WebhookDelivery.DeliveryStatus status;
    private int    attempts;
    private Instant lastAttemptAt;
    private Integer responseStatus;
    private Instant createdAt;
}
