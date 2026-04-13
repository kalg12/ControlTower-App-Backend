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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository         clientRepository;
    private final ClientBranchRepository   branchRepository;
    private final ClientContactRepository  contactRepository;
    private final TenantRepository         tenantRepository;

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
        client.setWebsite(request.getWebsite());
        client.setIndustry(request.getIndustry());
        if (StringUtils.hasText(request.getSegment())) {
            client.setSegment(Client.ClientSegment.valueOf(request.getSegment().toUpperCase()));
        }

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
        if (request.getNotes()   != null)                client.setNotes(request.getNotes());
        if (request.getWebsite() != null)                client.setWebsite(request.getWebsite());
        if (request.getIndustry() != null)               client.setIndustry(request.getIndustry());
        if (StringUtils.hasText(request.getSegment())) {
            client.setSegment(Client.ClientSegment.valueOf(request.getSegment().toUpperCase()));
        }

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

    // ── Contacts ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ContactResponse> listContacts(UUID clientId) {
        UUID tenantId = TenantContext.getTenantId();
        clientRepository.findByIdAndTenantIdAndDeletedAtIsNull(clientId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", clientId));
        return contactRepository.findByClientIdOrderByPrimaryDescCreatedAtAsc(clientId)
                .stream().map(this::toContactResponse).collect(Collectors.toList());
    }

    @Transactional
    public ContactResponse addContact(UUID clientId, ContactRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        Client client = clientRepository.findByIdAndTenantIdAndDeletedAtIsNull(clientId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", clientId));

        // If new contact is primary, demote any existing primary
        if (request.isPrimary()) {
            contactRepository.findByClientIdOrderByPrimaryDescCreatedAtAsc(clientId)
                    .stream().filter(ClientContact::isPrimary)
                    .forEach(c -> { c.setPrimary(false); contactRepository.save(c); });
        }

        ClientContact contact = new ClientContact();
        contact.setClient(client);
        contact.setTenant(client.getTenant());
        contact.setFullName(request.getFullName());
        contact.setEmail(request.getEmail());
        contact.setPhone(request.getPhone());
        contact.setNotes(request.getNotes());
        contact.setPrimary(request.isPrimary());
        if (StringUtils.hasText(request.getRole())) {
            contact.setRole(ClientContact.ContactRole.valueOf(request.getRole().toUpperCase()));
        }
        return toContactResponse(contactRepository.save(contact));
    }

    @Transactional
    public ContactResponse updateContact(UUID clientId, UUID contactId, ContactRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        clientRepository.findByIdAndTenantIdAndDeletedAtIsNull(clientId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", clientId));
        ClientContact contact = contactRepository.findByIdAndClientId(contactId, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("ClientContact", contactId));

        if (request.isPrimary() && !contact.isPrimary()) {
            contactRepository.findByClientIdOrderByPrimaryDescCreatedAtAsc(clientId)
                    .stream().filter(ClientContact::isPrimary)
                    .forEach(c -> { c.setPrimary(false); contactRepository.save(c); });
        }

        contact.setFullName(request.getFullName());
        if (request.getEmail()  != null) contact.setEmail(request.getEmail());
        if (request.getPhone()  != null) contact.setPhone(request.getPhone());
        if (request.getNotes()  != null) contact.setNotes(request.getNotes());
        contact.setPrimary(request.isPrimary());
        if (StringUtils.hasText(request.getRole())) {
            contact.setRole(ClientContact.ContactRole.valueOf(request.getRole().toUpperCase()));
        }
        return toContactResponse(contactRepository.save(contact));
    }

    @Transactional
    public void deleteContact(UUID clientId, UUID contactId) {
        UUID tenantId = TenantContext.getTenantId();
        clientRepository.findByIdAndTenantIdAndDeletedAtIsNull(clientId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Client", clientId));
        ClientContact contact = contactRepository.findByIdAndClientId(contactId, clientId)
                .orElseThrow(() -> new ResourceNotFoundException("ClientContact", contactId));
        contactRepository.delete(contact);
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
        long contactCount = contactRepository.findByClientIdOrderByPrimaryDescCreatedAtAsc(c.getId()).size();
        long branchCount = c.getBranches() != null ? c.getBranches().size() : 0;

        return ClientResponse.builder()
                .id(c.getId())
                .tenantId(c.getTenant().getId())
                .name(c.getName())
                .legalName(c.getLegalName())
                .taxId(c.getTaxId())
                .country(c.getCountry())
                .status(c.getStatus().name())
                .notes(c.getNotes())
                .website(c.getWebsite())
                .industry(c.getIndustry())
                .segment(c.getSegment() != null ? c.getSegment().name() : null)
                .accountOwnerId(c.getAccountOwnerId())
                .healthScore(c.getHealthScore())
                .totalRevenue(c.getTotalRevenue())
                .contactCount(contactCount)
                .branchCount(branchCount)
                .createdAt(c.getCreatedAt())
                .build();
    }

    private ContactResponse toContactResponse(ClientContact c) {
        return ContactResponse.builder()
                .id(c.getId())
                .clientId(c.getClient().getId())
                .fullName(c.getFullName())
                .email(c.getEmail())
                .phone(c.getPhone())
                .role(c.getRole().name())
                .primary(c.isPrimary())
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
