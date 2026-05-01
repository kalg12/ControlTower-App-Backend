package com.controltower.app.health.api.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Payload sent by a client system on each heartbeat ping.
 */
@Getter
@Setter
public class HeartbeatRequest {

    private String status;
    private Integer latencyMs;
    private String version;
    private String metadata;
}
