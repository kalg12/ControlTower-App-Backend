package com.controltower.app.support.infrastructure;

import com.controltower.app.support.domain.PosTicketWebhookEvent;
import com.controltower.app.integrations.domain.IntegrationEndpointRepository;
import com.controltower.app.integrations.domain.IntegrationEndpoint;
import com.controltower.app.shared.infrastructure.AesEncryptor;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

        PosWebhookService service = new PosWebhookService(
                mock(IntegrationEndpointRepository.class), mock(AesEncryptor.class));
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

    @Test
    void usesEndpointSpecificSecretInsteadOfGlobalFallback() throws Exception {
        AtomicReference<String> secretHeader = new AtomicReference<>();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/support/webhooks/ct", exchange -> {
            secretHeader.set(exchange.getRequestHeaders().getFirst("X-Webhook-Secret"));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        UUID endpointId = UUID.randomUUID();
        IntegrationEndpoint endpoint = new IntegrationEndpoint();
        endpoint.setWebhookSecret("encrypted-secret");
        IntegrationEndpointRepository endpointRepository = mock(IntegrationEndpointRepository.class);
        AesEncryptor encryptor = mock(AesEncryptor.class);
        when(endpointRepository.findById(endpointId)).thenReturn(Optional.of(endpoint));
        when(encryptor.decrypt("encrypted-secret")).thenReturn("endpoint-secret");

        PosWebhookService service = new PosWebhookService(endpointRepository, encryptor);
        ReflectionTestUtils.setField(service, "posWebhookSecret", "global-secret");
        String callback = "http://127.0.0.1:" + server.getAddress().getPort() + "/support/webhooks/ct";

        service.onPosTicketWebhook(PosTicketWebhookEvent.statusChange(
                "pos-ticket-2", callback, endpointId, "IN_PROGRESS"));

        assertThat(secretHeader).hasValue("endpoint-secret");
    }
}
