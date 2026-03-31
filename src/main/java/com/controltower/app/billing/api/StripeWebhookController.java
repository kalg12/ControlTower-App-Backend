package com.controltower.app.billing.api;

import com.controltower.app.billing.application.StripeWebhookService;
import com.controltower.app.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Receives Stripe webhook events.
 * Public endpoint — signature verified internally via stripe-java SDK.
 */
@Tag(name = "Billing", description = "Stripe webhook receiver (public — signature verified internally)")
@RestController
@RequestMapping("/api/v1/billing/stripe")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final StripeWebhookService webhookService;

    @Operation(summary = "Stripe webhook receiver", description = "Receives webhook events from Stripe. Public endpoint — the request signature is verified internally using the Stripe-Signature header.")
    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Void>> webhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {
        webhookService.handle(payload, sigHeader);
        return ResponseEntity.ok(ApiResponse.ok("Webhook received"));
    }
}
