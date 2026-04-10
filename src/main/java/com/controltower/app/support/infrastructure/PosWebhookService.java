package com.controltower.app.support.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Sends fire-and-forget webhook notifications to the POS Backend when
 * relevant events occur on POS-origin tickets (status change, new operator comment).
 *
 * Configure via env vars:
 *   POS_WEBHOOK_URL    — base URL of POS Backend (e.g. http://pos-backend:3000)
 *   POS_WEBHOOK_SECRET — shared secret sent in X-Webhook-Secret header
 */
@Slf4j
@Service
public class PosWebhookService {

    @Value("${pos.webhook.url:}")
    private String posWebhookUrl;

    @Value("${pos.webhook.secret:}")
    private String posWebhookSecret;

    private final RestClient restClient = RestClient.create();

    /** Called after a POS ticket status changes. */
    @Async
    public void notifyStatusChange(String posTicketId, String newCtStatus) {
        if (posWebhookUrl == null || posWebhookUrl.isBlank()) return;
        send(Map.of(
                "type", "STATUS_CHANGE",
                "posTicketId", posTicketId,
                "ctStatus", newCtStatus
        ));
    }

    /** Called after an operator adds a public comment on a POS ticket. */
    @Async
    public void notifyNewComment(String posTicketId, String content, String senderName) {
        if (posWebhookUrl == null || posWebhookUrl.isBlank()) return;
        send(Map.of(
                "type", "NEW_COMMENT",
                "posTicketId", posTicketId,
                "content", content,
                "senderName", senderName != null ? senderName : "Operador CT"
        ));
    }

    private void send(Map<String, String> payload) {
        try {
            restClient.post()
                    .uri(posWebhookUrl + "/support/webhooks/ct")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Webhook-Secret", posWebhookSecret != null ? posWebhookSecret : "")
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("POS webhook sent: type={} posTicketId={}", payload.get("type"), payload.get("posTicketId"));
        } catch (Exception e) {
            log.warn("POS webhook failed (type={}, posTicketId={}): {}",
                    payload.get("type"), payload.get("posTicketId"), e.getMessage());
        }
    }
}
