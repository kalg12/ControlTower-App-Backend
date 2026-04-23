package com.controltower.app.proposals.api;

import com.controltower.app.proposals.api.dto.ProposalResponse;
import com.controltower.app.proposals.application.ProposalService;
import com.controltower.app.proposals.domain.ProposalStatus;
import com.controltower.app.shared.response.ApiResponse;
import com.controltower.app.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Proposals", description = "Proposals by client")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/clients/{clientId}/proposals")
@RequiredArgsConstructor
public class ClientProposalController {

    private final ProposalService proposalService;

    @Operation(summary = "List proposals for a specific client")
    @GetMapping
    @PreAuthorize("hasAuthority('proposal:read')")
    public ResponseEntity<ApiResponse<PageResponse<ProposalResponse>>> listByClient(
            @PathVariable UUID clientId,
            @RequestParam(required = false) ProposalStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(proposalService.listByClient(clientId, status, pageable))));
    }
}
