package com.controltower.app.kb.api.dto;

import com.controltower.app.kb.domain.KbStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Schema(description = "Payload for creating or updating a knowledge base article")
@Getter
@Setter
public class KbArticleRequest {

    @NotBlank
    private String title;

    private String content;

    private String category;

    @Schema(description = "List of tags")
    private List<String> tags;

    private KbStatus status;
}
