package com.controltower.app.billing.application;

import com.controltower.app.billing.domain.*;
import com.controltower.app.licenses.application.LicenseService;
import com.controltower.app.licenses.domain.LicenseRepository;
import com.stripe.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BillingEventProcessor {

    private final StripeCustomerRepository stripeCustomerRepository;
    private final BillingEventRepository   billingEventRepository;
    private final LicenseService           licenseService;
    private final LicenseRepository        licenseRepository;

    public void process(com.stripe.model.Event stripeEvent, StripeWebhookEvent webhookEvent) {
        switch (stripeEvent.getType()) {
            case "customer.subscription.created",
                 "customer.subscription.updated" -> handleSubscriptionUpdated(stripeEvent);
            case "customer.subscription.deleted"  -> handleSubscriptionDeleted(stripeEvent);
            case "invoice.paid"                   -> handleInvoicePaid(stripeEvent);
            case "invoice.payment_failed"         -> handlePaymentFailed(stripeEvent);
            default -> {
                log.debug("Unhandled Stripe event type: {}", stripeEvent.getType());
                webhookEvent.markSkipped();
            }
        }
    }

    private void handleSubscriptionUpdated(com.stripe.model.Event event) {
        Subscription sub = (Subscription) event.getDataObjectDeserializer()
                .getObject().orElse(null);
        if (sub == null) return;

        stripeCustomerRepository.findByStripeCustomerId(sub.getCustomer())
                .ifPresent(customer -> {
                    customer.setStripeSubscriptionId(sub.getId());
                    stripeCustomerRepository.save(customer);

                    licenseRepository.findByClientIdAndDeletedAtIsNull(customer.getClientId())
                            .ifPresent(license -> {
                                Instant periodEnd = Instant.ofEpochSecond(
                                        sub.getCurrentPeriodEnd());
                                license.activate(periodEnd);
                                licenseRepository.save(license);
                                record(customer.getTenantId(), customer.getClientId(),
                                        "SUBSCRIPTION_UPDATED", null, null, event.getId());
                            });
                });
    }

    private void handleSubscriptionDeleted(com.stripe.model.Event event) {
        Subscription sub = (Subscription) event.getDataObjectDeserializer()
                .getObject().orElse(null);
        if (sub == null) return;

        stripeCustomerRepository.findByStripeCustomerId(sub.getCustomer())
                .ifPresent(customer -> {
                    licenseRepository.findByClientIdAndDeletedAtIsNull(customer.getClientId())
                            .ifPresent(license -> {
                                license.cancel();
                                licenseRepository.save(license);
                                record(customer.getTenantId(), customer.getClientId(),
                                        "SUBSCRIPTION_CANCELLED", null, null, event.getId());
                            });
                });
    }

    private void handleInvoicePaid(com.stripe.model.Event event) {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer()
                .getObject().orElse(null);
        if (invoice == null) return;

        stripeCustomerRepository.findByStripeCustomerId(invoice.getCustomer())
                .ifPresent(customer -> {
                    BigDecimal amount = BigDecimal.valueOf(invoice.getAmountPaid())
                            .movePointLeft(2); // Stripe uses cents
                    record(customer.getTenantId(), customer.getClientId(),
                            "INVOICE_PAID", amount, invoice.getCurrency(), event.getId());
                });
    }

    private void handlePaymentFailed(com.stripe.model.Event event) {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer()
                .getObject().orElse(null);
        if (invoice == null) return;

        stripeCustomerRepository.findByStripeCustomerId(invoice.getCustomer())
                .ifPresent(customer -> {
                    // Enter grace period on first payment failure
                    licenseRepository.findByClientIdAndDeletedAtIsNull(customer.getClientId())
                            .ifPresent(license -> {
                                license.enterGracePeriod(Instant.now().plus(7, ChronoUnit.DAYS));
                                licenseRepository.save(license);
                            });
                    record(customer.getTenantId(), customer.getClientId(),
                            "PAYMENT_FAILED", null, null, event.getId());
                });
    }

    private void record(UUID tenantId, UUID clientId, String eventType,
                        BigDecimal amount, String currency, String stripeEventId) {
        BillingEvent be = new BillingEvent();
        be.setTenantId(tenantId);
        be.setClientId(clientId);
        be.setEventType(eventType);
        be.setAmount(amount);
        be.setCurrency(currency);
        be.setStripeEventId(stripeEventId);
        billingEventRepository.save(be);
    }
}
