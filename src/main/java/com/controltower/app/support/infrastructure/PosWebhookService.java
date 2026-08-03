package com.controltower.app.support.infrastructure;

import com.controltower.app.support.domain.PosTicketWebhookEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.Map;

/**
 * Sends fire-and-forget webhook notifications to the POS Backend when
 * relevant events occur on POS-origin tickets (status change, new operator comment).
 *
 * The callback URL is stored per-ticket in posContext["callbackUrl"], so each POS
 * instance receives notifications on its own URL — enabling multi-tenant POS deployments.
 *
 * Configure via env var:
 *   POS_WEBHOOK_SECRET — shared secret sent in X-Webhook-Secret header
 */
@Slf4j
@Service
public class PosWebhookService {

    @Value("${pos.webhook.secret:}")
    private String posWebhookSecret;

    private final RestClient restClient = RestClient.create();

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPosTicketWebhook(PosTicketWebhookEvent event) {
        if (event.getCallbackUrl() == null || event.getCallbackUrl().isBlank()) {
            log.warn("POS webhook skipped: callbackUrl missing (type={}, posTicketId={})",
                    event.getType(), event.getPosTicketId());
            return;
        }
        if (posWebhookSecret == null || posWebhookSecret.isBlank()) {
            log.error("POS webhook skipped: POS_WEBHOOK_SECRET is not configured (type={}, posTicketId={}, url={})",
                    event.getType(), event.getPosTicketId(), event.getCallbackUrl());
            return;
        }

        Map<String, String> payload = new HashMap<>();
        payload.put("type", event.getType().name());
        payload.put("posTicketId", event.getPosTicketId());
        payload.put("occurredAt", event.getOccurredAt().toString());
        if (event.getCtStatus() != null) payload.put("ctStatus", event.getCtStatus());
        if (event.getCommentId() != null) payload.put("commentId", event.getCommentId().toString());
        if (event.getContent() != null) payload.put("content", event.getContent());
        if (event.getSenderName() != null) payload.put("senderName", event.getSenderName());
        send(event.getCallbackUrl(), payload);
    }

    private void send(String callbackUrl, Map<String, String> payload) {
        final int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                restClient.post()
                        .uri(callbackUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Webhook-Secret", posWebhookSecret)
                        .body(payload)
                        .retrieve()
                        .toBodilessEntity();
                log.info("POS webhook delivered (url={}, type={}, posTicketId={}, attempt={})",
                        callbackUrl, payload.get("type"), payload.get("posTicketId"), attempt);
                return;
            } catch (Exception ex) {
                String detail = ex instanceof RestClientResponseException responseException
                        ? "HTTP " + responseException.getStatusCode().value() + ": " + responseException.getResponseBodyAsString()
                        : ex.getMessage();
                if (attempt == maxAttempts) {
                    log.error("POS webhook exhausted retries (url={}, type={}, posTicketId={}, attempts={}): {}",
                            callbackUrl, payload.get("type"), payload.get("posTicketId"), attempt, detail, ex);
                    return;
                }
                log.warn("POS webhook attempt failed (url={}, type={}, posTicketId={}, attempt={}): {}",
                        callbackUrl, payload.get("type"), payload.get("posTicketId"), attempt, detail);
                try {
                    Thread.sleep(250L * attempt);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    log.error("POS webhook retry interrupted (url={}, type={}, posTicketId={})",
                            callbackUrl, payload.get("type"), payload.get("posTicketId"));
                    return;
                }
            }
        }
    }
}
