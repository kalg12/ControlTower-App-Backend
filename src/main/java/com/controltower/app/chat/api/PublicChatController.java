package com.controltower.app.chat.api;

import com.controltower.app.chat.api.dto.*;
import com.controltower.app.chat.application.ChatService;
import com.controltower.app.chat.domain.ChatConversation;
import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.NoSuchElementException;

@Tag(name = "Public Chat", description = "Public endpoints for POS chat widget (no auth required)")
@RestController
@RequestMapping("/api/v1/public/chat")
@RequiredArgsConstructor
public class PublicChatController {

    private final ChatService chatService;

    @Operation(summary = "Check agent availability for a tenant")
    @GetMapping("/availability")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAvailability(
            @RequestParam UUID tenantId) {
        return ResponseEntity.ok(ApiResponse.ok(chatService.getAvailability(tenantId)));
    }

    @Operation(summary = "Start a chat conversation (visitor)")
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<StartChatResponse>> startChat(
            @Valid @RequestBody StartChatRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(chatService.startConversation(req)));
    }

    @Operation(summary = "Get messages for a conversation (visitor auth via visitorToken param)")
    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<ApiResponse<PageResponse<ChatMessageResponse>>> getMessages(
            @PathVariable UUID id,
            @RequestParam UUID visitorToken,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        // Validate visitor token belongs to this conversation
        ChatConversation conv = chatService.requireConversationByToken(visitorToken);
        if (!conv.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Invalid visitor token for this conversation"));
        }

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(chatService.getMessages(id, pageable))));
    }

    @Operation(summary = "Get conversation status for visitor (re-sync after STOMP reconnect)")
    @GetMapping("/conversations/{id}")
    public ResponseEntity<ApiResponse<PublicConversationResponse>> getConversation(
            @PathVariable UUID id,
            @RequestParam UUID visitorToken) {
        try {
            PublicConversationResponse resp = chatService.getPublicConversation(id, visitorToken);
            return ResponseEntity.ok(ApiResponse.ok(resp));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Invalid visitor token for this conversation"));
        }
    }

    @Operation(summary = "Submit visitor rating after conversation closes")
    @PostMapping("/conversations/{id}/rate")
    public ResponseEntity<ApiResponse<Void>> rateConversation(
            @PathVariable UUID id,
            @Valid @RequestBody RateConversationRequest req) {
        try {
            chatService.rateConversation(id, req.visitorToken(), req.rating(), req.comment());
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
