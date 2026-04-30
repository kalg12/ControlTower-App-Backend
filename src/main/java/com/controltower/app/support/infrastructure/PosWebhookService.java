package com.controltower.app.support.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

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

    /** Called after a POS ticket status changes. */
    public void notifyStatusChange(String posTicketId, String callbackUrl, String newCtStatus) {
        if (callbackUrl == null || callbackUrl.isBlank()) return;
        send(callbackUrl, Map.of(
                "type", "STATUS_CHANGE",
                "posTicketId", posTicketId,
                "ctStatus", newCtStatus
        ));
    }

    /** Called after an operator adds a public comment on a POS ticket. */
    public void notifyNewComment(String posTicketId, String callbackUrl, String content, String senderName) {
        if (callbackUrl == null || callbackUrl.isBlank()) return;
        send(callbackUrl, Map.of(
                "type", "NEW_COMMENT",
                "posTicketId", posTicketId,
                "content", content,
                "senderName", senderName != null ? senderName : "Operador CT"
        ));
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
