package com.controltower.app.chat.api;

import com.controltower.app.chat.api.dto.*;
import com.controltower.app.chat.application.ChatService;
import com.controltower.app.chat.domain.ConversationStatus;
import com.controltower.app.shared.infrastructure.FileStorageService;
import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.UUID;

@Tag(name = "Chat", description = "Live chat inbox management for CT agents")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final FileStorageService fileStorageService;

    @Operation(summary = "List conversations")
    @GetMapping("/conversations")
    @PreAuthorize("hasAuthority('chat:read')")
    public ResponseEntity<ApiResponse<PageResponse<ChatConversationResponse>>> listConversations(
            @RequestParam(required = false) ConversationStatus status,
            @RequestParam(required = false) UUID agentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(chatService.listConversations(status, agentId, pageable))));
    }

    @Operation(summary = "Get conversation with messages")
    @GetMapping("/conversations/{id}")
    @PreAuthorize("hasAuthority('chat:read')")
    public ResponseEntity<ApiResponse<ChatConversationResponse>> getConversation(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(chatService.getConversation(id)));
    }

    @Operation(summary = "Get paginated messages")
    @GetMapping("/conversations/{id}/messages")
    @PreAuthorize("hasAuthority('chat:read')")
    public ResponseEntity<ApiResponse<PageResponse<ChatMessageResponse>>> getMessages(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        chatService.markRead(id);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(
                chatService.getMessages(id, PageRequest.of(page, size, Sort.by("createdAt").ascending())))));
    }

    @Operation(summary = "Claim conversation (WAITING → ACTIVE)")
    @PostMapping("/conversations/{id}/claim")
    @PreAuthorize("hasAuthority('chat:write')")
    public ResponseEntity<ApiResponse<ChatConversationResponse>> claimConversation(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(chatService.claimConversation(id)));
    }

    @Operation(summary = "Transfer conversation to another agent")
    @PostMapping("/conversations/{id}/transfer")
    @PreAuthorize("hasAuthority('chat:manage')")
    public ResponseEntity<ApiResponse<ChatConversationResponse>> transferConversation(
            @PathVariable UUID id, @Valid @RequestBody TransferRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(chatService.transferConversation(id, req)));
    }

    @Operation(summary = "Close conversation")
    @PostMapping("/conversations/{id}/close")
    @PreAuthorize("hasAuthority('chat:write')")
    public ResponseEntity<ApiResponse<ChatConversationResponse>> closeConversation(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(chatService.closeConversation(id)));
    }

    @Operation(summary = "Archive conversation (CLOSED → ARCHIVED)")
    @PostMapping("/conversations/{id}/archive")
    @PreAuthorize("hasAuthority('chat:manage')")
    public ResponseEntity<ApiResponse<Void>> archiveConversation(@PathVariable UUID id) {
        chatService.archiveConversation(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "Delete conversation (soft delete)")
    @DeleteMapping("/conversations/{id}")
    @PreAuthorize("hasAuthority('chat:manage')")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(@PathVariable UUID id) {
        chatService.deleteConversation(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @Operation(summary = "Count waiting conversations (for badge)")
    @GetMapping("/unread-count")
    @PreAuthorize("hasAuthority('chat:read')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countWaiting() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("count", chatService.countWaiting())));
    }

    @Operation(summary = "Get quick replies for this tenant")
    @GetMapping("/quick-replies")
    @PreAuthorize("hasAuthority('chat:read')")
    public ResponseEntity<ApiResponse<List<ChatQuickReplyResponse>>> getQuickReplies() {
        List<ChatQuickReplyResponse> list = chatService.getQuickReplies().stream()
                .map(r -> new ChatQuickReplyResponse(r.getId(), r.getShortcut(), r.getContent()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @Operation(summary = "Upload attachment to conversation (agent)")
    @PostMapping("/conversations/{id}/attachments")
    @PreAuthorize("hasAuthority('chat:write')")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> uploadAttachment(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            Authentication auth) {
        String storageKey = fileStorageService.store(file, "chat/" + id);
        String publicUrl = "/api/v1/chat/attachments/" + storageKey;
        UUID agentId = UUID.fromString(auth.getName());
        ChatMessageResponse msg = chatService.sendMessageWithAttachment(id, agentId, publicUrl, file.getOriginalFilename());
        return ResponseEntity.ok(ApiResponse.ok(msg));
    }

    @Operation(summary = "Get current agent presence state")
    @GetMapping("/presence")
    @PreAuthorize("hasAuthority('chat:read')")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getMyPresence(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        boolean online = chatService.getMyPresence(userId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("online", online)));
    }

    @Operation(summary = "Set agent presence (online/offline for live chat)")
    @PostMapping("/presence")
    @PreAuthorize("hasAuthority('chat:read')")
    public ResponseEntity<ApiResponse<Void>> setPresence(
            Authentication auth,
            @RequestBody Map<String, Boolean> body) {
        UUID userId = UUID.fromString(auth.getName());
        boolean online = Boolean.TRUE.equals(body.get("online"));
        chatService.setPresence(userId, online);
        return ResponseEntity.ok(ApiResponse.ok("Presence updated", null));
    }

    @Operation(summary = "Serve chat attachment file")
    @GetMapping("/attachments/**")
    @PreAuthorize("hasAuthority('chat:read')")
    public ResponseEntity<Resource> serveAttachment(jakarta.servlet.http.HttpServletRequest request) {
        String path = request.getRequestURI().replaceFirst(".*/api/v1/chat/attachments/", "");
        Resource resource = fileStorageService.load(path);
        String contentType = "application/octet-stream";
        try { contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath()); } catch (Exception ignored) {}
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
