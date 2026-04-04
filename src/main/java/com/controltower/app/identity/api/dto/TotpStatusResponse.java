package com.controltower.app.identity.api.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TotpStatusResponse {

    private final boolean enabled;
    private final boolean setupStarted;
}
