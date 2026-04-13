package com.controltower.app.activity.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class UserActivityRequest {

    @NotBlank
    private String routePath;

    private String pageTitle;

    private Integer durationSeconds;

    private String fullUrl;

    private String sessionId;

    private Instant visitedAt;
}
