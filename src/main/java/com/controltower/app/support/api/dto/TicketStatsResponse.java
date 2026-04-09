package com.controltower.app.support.api.dto;

import lombok.Getter;
import lombok.AllArgsConstructor;

import java.util.Map;

@Getter
@AllArgsConstructor
public class TicketStatsResponse {
    private long total;
    private Map<String, Long> byStatus;
}
