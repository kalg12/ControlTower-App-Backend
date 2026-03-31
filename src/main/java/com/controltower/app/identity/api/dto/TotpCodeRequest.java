package com.controltower.app.identity.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TotpCodeRequest {

    @NotNull(message = "code is required")
    private Integer code;
}
