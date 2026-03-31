package com.controltower.app.identity.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TotpVerifyRequest {

    @NotBlank(message = "mfaToken is required")
    private String mfaToken;

    @NotNull(message = "code is required")
    private Integer code;
}
