package com.controltower.app.shared.infrastructure;

import com.controltower.app.shared.config.OpenWaProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Sends text alerts to the Comerza dev team WhatsApp group via OpenWA.
 *
 * Disabled by default (openwa.enabled=false). When disabled, the message is
 * logged instead of sent so you can verify formatting without a live session.
 *
 * Any error (session not ready, network issue, auth failure) is logged and
 * swallowed — this channel must never block or fail the calling operation.
 */
@Slf4j
@Service
public class OpenWaService {

    private final OpenWaProperties props;
    private final RestClient client;

    public OpenWaService(OpenWaProperties props) {
        this.props = props;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3_000);
        factory.setReadTimeout(8_000);

        this.client = RestClient.builder()
                .baseUrl(props.getBaseUrl())
                .requestFactory(factory)
                .build();
    }

    /**
     * Sends a plain-text alert to the configured dev group.
     * Safe to call from any thread, including @Async event listeners.
     */
    public void sendDevAlert(String message) {
        if (!props.isEnabled()) {
            log.info("DRY-RUN WhatsApp: {}", message);
            return;
        }

        if (isBlank(props.getApiKey()) || isBlank(props.getSession()) || isBlank(props.getDevGroupId())) {
            log.warn("OpenWA config incomplete — missing apiKey, session, or devGroupId. Alert skipped.");
            return;
        }

        try {
            if (!isSessionReady()) return;
            doSend(message);
        } catch (Exception e) {
            log.warn("OpenWA error sending alert: {}", e.getMessage());
        }
    }

    private boolean isSessionReady() {
        try {
            Map<?, ?> response = client.get()
                    .uri("/api/sessions/{session}", props.getSession())
                    .header("X-API-Key", props.getApiKey())
                    .retrieve()
                    .body(Map.class);

            if (response == null) {
                log.warn("OpenWA session check returned null for session '{}'", props.getSession());
                return false;
            }

            String status = (String) response.get("status");
            if (!"ready".equals(status)) {
                log.warn("OpenWA session '{}' is not ready (status={}). Alert skipped.", props.getSession(), status);
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn("OpenWA session check failed for '{}': {}", props.getSession(), e.getMessage());
            return false;
        }
    }

    private void doSend(String message) {
        Map<String, String> body = Map.of(
                "chatId", props.getDevGroupId(),
                "text", message
        );

        Map<?, ?> response = client.post()
                .uri("/api/sessions/{session}/messages/send-text", props.getSession())
                .header("X-API-Key", props.getApiKey())
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(Map.class);

        String messageId = response != null ? (String) response.get("messageId") : null;
        log.info("OpenWA alert sent to group '{}': messageId={}", props.getDevGroupId(), messageId);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
