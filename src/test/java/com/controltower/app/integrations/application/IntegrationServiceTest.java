package com.controltower.app.integrations.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrationServiceTest {

    @Test
    void derivesWebhookCallbackFromHealthUrl() {
        assertThat(IntegrationService.derivePosCallbackUrl("https://pos.example.com/health"))
                .isEqualTo("https://pos.example.com/support/webhooks/ct");
        assertThat(IntegrationService.derivePosCallbackUrl("https://pos.example.com:8443/api/health/check"))
                .isEqualTo("https://pos.example.com:8443/support/webhooks/ct");
    }

    @Test
    void rejectsUnusableCallbackOrigins() {
        assertThat(IntegrationService.derivePosCallbackUrl(null)).isNull();
        assertThat(IntegrationService.derivePosCallbackUrl("not-a-url")).isNull();
        assertThat(IntegrationService.derivePosCallbackUrl("file:///tmp/health")).isNull();
    }
}
