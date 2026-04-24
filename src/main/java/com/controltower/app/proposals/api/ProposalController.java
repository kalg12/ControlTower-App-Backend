package com.controltower.app.proposals.api;

import com.controltower.app.proposals.api.dto.*;
import com.controltower.app.proposals.application.ProposalPdfService;
import com.controltower.app.proposals.application.ProposalService;
import com.controltower.app.proposals.domain.ProposalStatus;
import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@Tag(name = "Proposals", description = "Economic proposals lifecycle management")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/proposals")
@RequiredArgsConstructor
public class ProposalController {

    private final ProposalService proposalService;
    private final ProposalPdfService proposalPdfService;

    @Operation(summary = "List proposals")
    @GetMapping
    @PreAuthorize("hasAuthority('proposal:read')")
    public ResponseEntity<ApiResponse<PageResponse<ProposalResponse>>> listProposals(
            @RequestParam(required = false) ProposalStatus status,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(proposalService.listProposals(status, clientId, from, to, pageable))));
    }

    @Operation(summary = "Get proposal by ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('proposal:read')")
    public ResponseEntity<ApiResponse<ProposalResponse>> getProposal(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(proposalService.getProposal(id)));
    }

    @Operation(summary = "Create proposal (DRAFT)")
    @PostMapping
    @PreAuthorize("hasAuthority('proposal:write')")
    public ResponseEntity<ApiResponse<ProposalResponse>> createProposal(
            @Valid @RequestBody ProposalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(proposalService.createProposal(request)));
    }

    @Operation(summary = "Update proposal (DRAFT only)")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('proposal:write')")
    public ResponseEntity<ApiResponse<ProposalResponse>> updateProposal(
            @PathVariable UUID id,
            @Valid @RequestBody ProposalRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(proposalService.updateProposal(id, request)));
    }

    @Operation(summary = "Delete proposal (DRAFT only)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('proposal:write')")
    public ResponseEntity<Void> deleteProposal(@PathVariable UUID id) {
        proposalService.deleteProposal(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Send proposal to client via email (DRAFT → SENT)")
    @PostMapping("/{id}/send")
    @PreAuthorize("hasAuthority('proposal:write')")
    public ResponseEntity<ApiResponse<ProposalResponse>> sendProposal(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(proposalService.sendProposal(id)));
    }

    @Operation(summary = "Accept proposal (SENT/VIEWED → ACCEPTED)")
    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAuthority('proposal:write')")
    public ResponseEntity<ApiResponse<ProposalResponse>> acceptProposal(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(proposalService.acceptProposal(id)));
    }

    @Operation(summary = "Reject proposal (SENT/VIEWED → REJECTED)")
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('proposal:write')")
    public ResponseEntity<ApiResponse<ProposalResponse>> rejectProposal(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(proposalService.rejectProposal(id)));
    }

    @Operation(summary = "Mark proposal as viewed (SENT → VIEWED)")
    @PostMapping("/{id}/mark-viewed")
    @PreAuthorize("hasAuthority('proposal:write')")
    public ResponseEntity<ApiResponse<ProposalResponse>> markViewed(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(proposalService.markViewed(id)));
    }

    @Operation(summary = "Download proposal as PDF")
    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAuthority('proposal:read')")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id) {
        ProposalResponse proposal = proposalService.getProposal(id);
        byte[] pdf = proposalPdfService.generate(proposal);
        String filename = "propuesta-" + proposal.number().replace("/", "-") + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(pdf);
    }
}
