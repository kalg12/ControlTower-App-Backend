package com.controltower.app.support.infrastructure;

import com.controltower.app.support.domain.PosTicketWebhookEvent;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class PosWebhookServiceTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void retriesAndDeliversCommittedCommentPayload() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        AtomicReference<String> requestBody = new AtomicReference<>();
        AtomicReference<String> secretHeader = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/support/webhooks/ct", exchange -> {
            int attempt = attempts.incrementAndGet();
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            secretHeader.set(exchange.getRequestHeaders().getFirst("X-Webhook-Secret"));
            exchange.sendResponseHeaders(attempt < 3 ? 503 : 200, -1);
            exchange.close();
        });
        server.start();

        PosWebhookService service = new PosWebhookService();
        ReflectionTestUtils.setField(service, "posWebhookSecret", "shared-secret");
        UUID commentId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-08-03T05:00:00Z");
        String callback = "http://127.0.0.1:" + server.getAddress().getPort() + "/support/webhooks/ct";

        service.onPosTicketWebhook(PosTicketWebhookEvent.newComment(
                "pos-ticket-1", callback, commentId, "Fixed", "Agent", occurredAt));

        assertThat(attempts).hasValue(3);
        assertThat(secretHeader).hasValue("shared-secret");
        assertThat(requestBody.get())
                .contains("\"type\":\"NEW_COMMENT\"")
                .contains(commentId.toString())
                .contains("\"content\":\"Fixed\"");
    }
}
