package com.controltower.app.integrations.infrastructure;

import com.controltower.app.integrations.domain.WebhookDelivery;
import com.controltower.app.integrations.domain.WebhookDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Periodically retries PENDING webhook deliveries (up to 3 attempts).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookRetryScheduler {

    private final WebhookDeliveryRepository deliveryRepository;

    private final RestClient restClient = RestClient.create();

    @Scheduled(fixedDelay = 300_000) // every 5 minutes
    public void retryPending() {
        List<WebhookDelivery> retryable = deliveryRepository.findRetryable();
        if (retryable.isEmpty()) {
            return;
        }
        log.info("Retrying {} pending webhook deliveries", retryable.size());

        for (WebhookDelivery delivery : retryable) {
            int statusCode = 0;
            try {
                restClient.post()
                        .uri(delivery.getUrl())
                        .body(delivery.getPayload())
                        .retrieve()
                        .toBodilessEntity();
                statusCode = 200;
                log.info("Webhook delivery {} succeeded for URL {}", delivery.getId(), delivery.getUrl());
            } catch (RestClientResponseException ex) {
                statusCode = ex.getStatusCode().value();
                log.warn("Webhook delivery {} failed with status {} for URL {}",
                        delivery.getId(), statusCode, delivery.getUrl());
            } catch (Exception ex) {
                statusCode = 0;
                log.warn("Webhook delivery {} error for URL {}: {}",
                        delivery.getId(), delivery.getUrl(), ex.getMessage());
            }

            delivery.recordAttempt(statusCode);
            deliveryRepository.save(delivery);
        }
    }
}
