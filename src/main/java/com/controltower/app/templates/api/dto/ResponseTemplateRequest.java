package com.controltower.app.templates.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "Payload for creating or updating a response template")
@Getter
@Setter
public class ResponseTemplateRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String body;
    private String category;
    private String shortcut;
}
