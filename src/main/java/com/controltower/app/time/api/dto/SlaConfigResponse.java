package com.controltower.app.time.api.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SlaConfigResponse {
    /** SLA window in hours for LOW priority tickets. */
    private final int low;
    /** SLA window in hours for MEDIUM priority tickets. */
    private final int medium;
    /** SLA window in hours for HIGH priority tickets. */
    private final int high;
    /** SLA window in hours for CRITICAL priority tickets. */
    private final int critical;
}
