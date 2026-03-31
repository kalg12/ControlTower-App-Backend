package com.controltower.app.identity.api.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TotpSetupResponse {

    private final String secret;
    private final String qrUrl;
}
