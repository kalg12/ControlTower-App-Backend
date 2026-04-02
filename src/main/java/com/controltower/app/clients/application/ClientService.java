package com.controltower.app.clients.application;

import com.controltower.app.clients.api.dto.*;
import com.controltower.app.clients.domain.*;
import com.controltower.app.identity.domain.Tenant;
import com.controltower.app.identity.domain.TenantRepository;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientBranchRepository branchRepository;
    private final TenantRepository tenantRepository;

    // ── Clients ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ClientResponse> listClients(String search, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        if (StringUtils.hasText(search)) {
            return clientRepository.searchByTenant(tenantId, search, pageable).map(this::toClientResponse);
        }
        return clientRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable).map(this::toClientResponse);
    }

    @Transactional(readOnly = true)
    public ClientResponse getClient(UUID clientId) {
        UUID tenantId = TenantContext.getTenantId();
        Client client = clientRepository.findByIdAndTenantIdAndDeletedAtIsNull(clientId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", clientId));
        return toClientResponse(client);
    }

    @Transactional
    public ClientResponse createClient(ClientRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant", tenantId));

        Client client = new Client();
        client.setTenant(tenant);
        client.setName(request.getName());
        client.setLegalName(request.getLegalName());
        client.setTaxId(request.getTaxId());
        client.setCountry(StringUtils.hasText(request.getCountry()) ? request.getCountry() : "México");
        client.setNotes(request.getNotes());

        return toClientResponse(clientRepository.save(client));
    }

    @Transactional
    public ClientResponse updateClient(UUID clientId, ClientRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        Client client = clientRepository.findByIdAndTenantIdAndDeletedAtIsNull(clientId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", clientId));

        client.setName(request.getName());
        if (StringUtils.hasText(request.getLegalName())) client.setLegalName(request.getLegalName());
        if (StringUtils.hasText(request.getTaxId()))     client.setTaxId(request.getTaxId());
        if (StringUtils.hasText(request.getCountry()))   client.setCountry(request.getCountry());
        if (StringUtils.hasText(request.getNotes()))     client.setNotes(request.getNotes());

        return toClientResponse(clientRepository.save(client));
    }

    @Transactional
    public void deleteClient(UUID clientId) {
        UUID tenantId = TenantContext.getTenantId();
        Client client = clientRepository.findByIdAndTenantIdAndDeletedAtIsNull(clientId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", clientId));
        client.softDelete();
        clientRepository.save(client);
    }

    // ── Branches ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<BranchResponse> listBranches(UUID clientId) {
        UUID tenantId = TenantContext.getTenantId();
        // Verify client belongs to tenant
        clientRepository.findByIdAndTenantIdAndDeletedAtIsNull(clientId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", clientId));
        return branchRepository.findByClientIdAndDeletedAtIsNull(clientId)
                .stream().map(this::toBranchResponse).toList();
    }

    @Transactional
    public BranchResponse createBranch(UUID clientId, BranchRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        Client client = clientRepository.findByIdAndTenantIdAndDeletedAtIsNull(clientId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", clientId));
        Tenant tenant = client.getTenant();

        ClientBranch branch = new ClientBranch();
        branch.setClient(client);
        branch.setTenant(tenant);
        branch.setName(request.getName());
        branch.setAddress(request.getAddress());
        branch.setCity(request.getCity());
        branch.setCountry(request.getCountry());
        branch.setLatitude(request.getLatitude());
        branch.setLongitude(request.getLongitude());
        branch.setSlug(resolveSlug(request.getSlug(), request.getName(), clientId));
        if (StringUtils.hasText(request.getTimezone())) branch.setTimezone(request.getTimezone());

        return toBranchResponse(branchRepository.save(branch));
    }

    @Transactional
    public BranchResponse updateBranch(UUID clientId, UUID branchId, BranchRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        // Verify client belongs to tenant
        clientRepository.findByIdAndTenantIdAndDeletedAtIsNull(clientId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", clientId));
        ClientBranch branch = branchRepository.findByIdAndTenantIdAndDeletedAtIsNull(branchId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ClientBranch", branchId));

        if (StringUtils.hasText(request.getName()))    branch.setName(request.getName());
        if (StringUtils.hasText(request.getAddress())) branch.setAddress(request.getAddress());
        if (StringUtils.hasText(request.getCity()))    branch.setCity(request.getCity());
        if (StringUtils.hasText(request.getCountry())) branch.setCountry(request.getCountry());
        if (StringUtils.hasText(request.getTimezone())) branch.setTimezone(request.getTimezone());
        if (request.getLatitude()  != null) branch.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) branch.setLongitude(request.getLongitude());
        if (request.getActive() != null) {
            branch.setStatus(request.getActive()
                    ? ClientBranch.BranchStatus.ACTIVE
                    : ClientBranch.BranchStatus.INACTIVE);
        }
        return toBranchResponse(branchRepository.save(branch));
    }

    @Transactional
    public void deleteBranch(UUID branchId) {
        UUID tenantId = TenantContext.getTenantId();
        ClientBranch branch = branchRepository.findByIdAndTenantIdAndDeletedAtIsNull(branchId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("ClientBranch", branchId));
        branch.softDelete();
        branchRepository.save(branch);
    }

    // ── Slug generation ───────────────────────────────────────────────

    /**
     * If the caller provides a slug, use it; otherwise derive one from the branch name.
     * Appends a random 4-char suffix to prevent collisions across the same client.
     * Result: lowercase alphanumeric + hyphens, max 60 chars.
     */
    private String resolveSlug(String providedSlug, String branchName, UUID clientId) {
        if (StringUtils.hasText(providedSlug)) {
            return providedSlug.toLowerCase().replaceAll("[^a-z0-9-]", "-");
        }
        String base = branchName.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        if (base.length() > 50) base = base.substring(0, 50);
        String suffix = Long.toHexString(System.currentTimeMillis()).substring(4);
        return base + "-" + suffix;
    }

    // ── Mapping ───────────────────────────────────────────────────────

    private ClientResponse toClientResponse(Client c) {
        return ClientResponse.builder()
                .id(c.getId())
                .tenantId(c.getTenant().getId())
                .name(c.getName())
                .legalName(c.getLegalName())
                .taxId(c.getTaxId())
                .country(c.getCountry())
                .status(c.getStatus().name())
                .notes(c.getNotes())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private BranchResponse toBranchResponse(ClientBranch b) {
        return BranchResponse.builder()
                .id(b.getId())
                .clientId(b.getClient().getId())
                .tenantId(b.getTenant().getId())
                .name(b.getName())
                .address(b.getAddress())
                .city(b.getCity())
                .country(b.getCountry())
                .latitude(b.getLatitude())
                .longitude(b.getLongitude())
                .slug(b.getSlug())
                .status(b.getStatus().name())
                .isActive(b.getStatus() == ClientBranch.BranchStatus.ACTIVE)
                .timezone(b.getTimezone())
                .createdAt(b.getCreatedAt())
                .build();
    }
}
