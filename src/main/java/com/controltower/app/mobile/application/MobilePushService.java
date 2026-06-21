package com.controltower.app.mobile.application;

import com.controltower.app.mobile.domain.MobilePushToken;
import com.controltower.app.mobile.domain.MobilePushTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sends push notifications via Expo Push API (https://exp.host/--/api/v2/push/send).
 * Expo handles delivery to both FCM (Android) and APNs (iOS).
 * No API key required for Expo Push Service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MobilePushService {

    private final MobilePushTokenRepository tokenRepo;
    private final RestTemplate restTemplate;

    @Value("${expo.push-api-url:https://exp.host/--/api/v2/push/send}")
    private String expoPushApiUrl;

    /** Sends a push notification to all active devices of a user. */
    public void sendToUser(UUID userId, String title, String body, Map<String, Object> data) {
        List<MobilePushToken> tokens = tokenRepo.findByUserIdAndActiveTrue(userId);
        if (tokens.isEmpty()) return;

        List<Map<String, Object>> messages = tokens.stream()
            .map(t -> buildMessage(t.getToken(), title, body, data))
            .toList();

        sendBatch(messages, tokens);
    }

    private void sendBatch(List<Map<String, Object>> messages, List<MobilePushToken> tokens) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");
            headers.set("Accept-Encoding", "gzip, deflate");

            HttpEntity<List<Map<String, Object>>> request = new HttpEntity<>(messages, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(expoPushApiUrl, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("Push sent to {} device(s)", messages.size());
            } else {
                log.warn("Expo Push API returned {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Failed to send push notifications: {}", e.getMessage());
        }
    }

    private Map<String, Object> buildMessage(String token, String title, String body, Map<String, Object> data) {
        return Map.of(
            "to", token,
            "title", title,
            "body", body,
            "data", data != null ? data : Map.of(),
            "sound", "default"
        );
    }
}
