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
        if (event.getCallbackUrl() == null || event.getCallbackUrl().isBlank()) return;

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
        try {
            restClient.post()
                    .uri(callbackUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Webhook-Secret", posWebhookSecret != null ? posWebhookSecret : "")
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("POS webhook sent to {}: type={} posTicketId={}", callbackUrl, payload.get("type"), payload.get("posTicketId"));
        } catch (Exception e) {
            log.warn("POS webhook failed (url={}, type={}, posTicketId={}): {}",
                    callbackUrl, payload.get("type"), payload.get("posTicketId"), e.getMessage());
        }
    }
}
