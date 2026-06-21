package com.controltower.app.ai.api;

import com.controltower.app.ai.api.dto.AiAssistRequest;
import com.controltower.app.ai.api.dto.AiAssistResponse;
import com.controltower.app.ai.application.AiAssistService;
import com.controltower.app.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI Assist", description = "AI-powered content generation and improvement")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiAssistController {

    private final AiAssistService aiAssistService;

    @Operation(summary = "Generate AI-assisted content (card prompts, ticket replies)")
    @PostMapping("/assist")
    @PreAuthorize("hasAuthority('ticket:read')")
    public ResponseEntity<ApiResponse<AiAssistResponse>> assist(
            @Valid @RequestBody AiAssistRequest request
    ) {
        String result = aiAssistService.assist(request.task(), request.context());
        return ResponseEntity.ok(ApiResponse.ok(new AiAssistResponse(result)));
    }
}
