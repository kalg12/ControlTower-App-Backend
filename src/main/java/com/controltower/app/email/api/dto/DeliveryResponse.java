package com.controltower.app.email.api.dto;

import com.controltower.app.email.domain.EmailDelivery;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DeliveryResponse(
    UUID id,
    UUID tenantId,
    UUID ticketId,
    String fromEmail,
    List<String> toEmail,
    String subject,
    String status,
    String deliveryType,
    int attempts,
    Instant lastAttemptAt,
    String errorMessage,
    Instant sentAt,
    Instant createdAt
) {
    public static DeliveryResponse from(EmailDelivery d) {
        return new DeliveryResponse(
            d.getId(), d.getTenantId(), d.getTicketId(),
            d.getFromEmail(),
            d.getToEmail() != null ? List.of(d.getToEmail()) : List.of(),
            d.getSubject(),
            d.getStatus().name(),
            d.getDeliveryType().name(),
            d.getAttempts(), d.getLastAttemptAt(),
            d.getErrorMessage(), d.getSentAt(), d.getCreatedAt()
        );
    }
}
