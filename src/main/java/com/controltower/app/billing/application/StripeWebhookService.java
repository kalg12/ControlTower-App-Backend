package com.controltower.app.billing.application;

import com.controltower.app.billing.domain.*;
import com.controltower.app.shared.annotation.Audited;
import com.controltower.app.shared.exception.ControlTowerException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeWebhookService {

    private final StripeWebhookEventRepository webhookEventRepository;
    private final BillingEventProcessor        billingEventProcessor;
    private final JsonMapper                   objectMapper;

    @Value("${app.stripe.webhook-secret:}")
    private String webhookSecret;

    @Transactional
    @Audited(action = "STRIPE_WEBHOOK_RECEIVED", resource = "StripeWebhookEvent")
    public void handle(String payload, String sigHeader) {
        // 1. Validate signature (skip if no secret configured — dev mode)
        Event stripeEvent = parseAndVerify(payload, sigHeader);
        String stripeEventId = stripeEvent.getId();

        // 2. Idempotency check
        if (webhookEventRepository.findByStripeEventId(stripeEventId).isPresent()) {
            log.info("Stripe event {} already processed — skipping", stripeEventId);
            return;
        }

        // 3. Persist raw event
        Map<String, Object> payloadMap = objectMapper.convertValue(
                stripeEvent, new TypeReference<>() {});

        StripeWebhookEvent webhookEvent = new StripeWebhookEvent();
        webhookEvent.setStripeEventId(stripeEventId);
        webhookEvent.setType(stripeEvent.getType());
        webhookEvent.setPayload(payloadMap);
        webhookEventRepository.save(webhookEvent);

        // 4. Process
        try {
            billingEventProcessor.process(stripeEvent, webhookEvent);
            webhookEvent.markProcessed();
        } catch (Exception e) {
            log.error("Failed to process Stripe event {}: {}", stripeEventId, e.getMessage(), e);
            webhookEvent.markFailed(e.getMessage());
        }
        webhookEventRepository.save(webhookEvent);
    }

    private Event parseAndVerify(String payload, String sigHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            // Dev mode: parse without signature verification
            try {
                return Event.GSON.fromJson(payload, Event.class);
            } catch (Exception e) {
                throw new ControlTowerException("Invalid Stripe event payload", HttpStatus.BAD_REQUEST);
            }
        }
        try {
            return Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new ControlTowerException("Invalid Stripe webhook signature", HttpStatus.UNAUTHORIZED);
        }
    }
}
