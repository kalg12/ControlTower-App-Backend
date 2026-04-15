package com.controltower.app.time.api.dto;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSlaConfigRequest {

    @Min(value = 1, message = "LOW SLA must be at least 1 hour")
    private Integer low;

    @Min(value = 1, message = "MEDIUM SLA must be at least 1 hour")
    private Integer medium;

    @Min(value = 1, message = "HIGH SLA must be at least 1 hour")
    private Integer high;

    @Min(value = 1, message = "CRITICAL SLA must be at least 1 hour")
    private Integer critical;
}
