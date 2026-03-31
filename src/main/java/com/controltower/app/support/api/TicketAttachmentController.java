package com.controltower.app.support.api;

import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.shared.infrastructure.FileStorageService;
import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.support.domain.TicketAttachment;
import com.controltower.app.support.domain.TicketAttachmentRepository;
import com.controltower.app.support.domain.TicketRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Tag(name = "Ticket Attachments", description = "File attachments for support tickets")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class TicketAttachmentController {

    private final TicketAttachmentRepository attachmentRepository;
    private final TicketRepository           ticketRepository;
    private final FileStorageService         fileStorageService;

    @PostMapping("/api/v1/tickets/{id}/attachments")
    @PreAuthorize("hasAuthority('ticket:write')")
    public ResponseEntity<ApiResponse<TicketAttachment>> upload(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails principal) {

        ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));

        String storageKey = fileStorageService.store(file, "tickets/" + id);

        TicketAttachment attachment = new TicketAttachment();
        attachment.setTicketId(id);
        attachment.setUploadedBy(principal != null ? UUID.fromString(principal.getUsername()) : null);
        attachment.setFileName(file.getOriginalFilename());
        attachment.setContentType(file.getContentType());
        attachment.setSizeBytes(file.getSize());
        attachment.setStorageKey(storageKey);

        TicketAttachment saved = attachmentRepository.save(attachment);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("File uploaded", saved));
    }

    @GetMapping("/api/v1/tickets/{id}/attachments")
    @PreAuthorize("hasAuthority('ticket:read')")
    public ResponseEntity<ApiResponse<List<TicketAttachment>>> list(@PathVariable UUID id) {
        ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
        List<TicketAttachment> attachments = attachmentRepository.findByTicketId(id);
        return ResponseEntity.ok(ApiResponse.ok(attachments));
    }

    @GetMapping("/api/v1/attachments/{attachmentId}")
    @PreAuthorize("hasAuthority('ticket:read')")
    public ResponseEntity<Resource> download(@PathVariable UUID attachmentId) {
        TicketAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", attachmentId));

        Resource resource = fileStorageService.load(attachment.getStorageKey());

        String contentType = attachment.getContentType() != null
                ? attachment.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + attachment.getFileName() + "\"")
                .body(resource);
    }

    @DeleteMapping("/api/v1/attachments/{attachmentId}")
    @PreAuthorize("hasAuthority('ticket:write')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID attachmentId) {
        TicketAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", attachmentId));

        fileStorageService.delete(attachment.getStorageKey());
        attachmentRepository.delete(attachment);
        return ResponseEntity.ok(ApiResponse.ok("Attachment deleted"));
    }
}
