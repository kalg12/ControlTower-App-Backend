package com.controltower.app.support.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddCommentRequest {

    @NotBlank(message = "Comment content is required")
    private String content;

    private boolean internal = false;
}
