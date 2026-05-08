package com.controltower.app.monitoring.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class LogEntryDto {

    @NotNull
    private String level; // WARN | ERROR | CRITICAL

    private String serviceName;

    @NotBlank
    private String message;

    private String stackTrace;

    private String businessName;

    private String source;

    private String timestamp;

    private Map<String, Object> metadata;
}
