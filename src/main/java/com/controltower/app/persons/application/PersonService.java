package com.controltower.app.persons.application;

import com.controltower.app.clients.domain.ClientRepository;
import com.controltower.app.identity.domain.UserRepository;
import com.controltower.app.persons.api.dto.PersonRequest;
import com.controltower.app.persons.api.dto.PersonResponse;
import com.controltower.app.persons.domain.Person;
import com.controltower.app.persons.domain.PersonRepository;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;
    private final ClientRepository clientRepository;
    private final UserRepository   userRepository;

    @Transactional(readOnly = true)
    public Page<PersonResponse> list(String search, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        Page<Person> page = StringUtils.hasText(search)
            ? personRepository.search(tenantId, search.trim(), pageable)
            : personRepository.findByTenantIdAndDeletedAtIsNull(tenantId, pageable);
        List<PersonResponse> content = page.getContent().stream()
            .map(this::toResponse).collect(Collectors.toList());
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PersonResponse get(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        Person p = personRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Person", id));
        return toResponse(p);
    }

    @Transactional
    public PersonResponse create(PersonRequest req) {
        UUID tenantId = TenantContext.getTenantId();
        Person p = new Person();
        p.setTenantId(tenantId);
        applyRequest(p, req);
        return toResponse(personRepository.save(p));
    }

    @Transactional
    public PersonResponse update(UUID id, PersonRequest req) {
        UUID tenantId = TenantContext.getTenantId();
        Person p = personRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Person", id));
        applyRequest(p, req);
        return toResponse(personRepository.save(p));
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        Person p = personRepository.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Person", id));
        p.softDelete();
        personRepository.save(p);
    }

    @Transactional(readOnly = true)
    public List<PersonResponse> listByClient(UUID clientId) {
        UUID tenantId = TenantContext.getTenantId();
        return personRepository.findByTenantIdAndClientIdAndDeletedAtIsNull(tenantId, clientId)
            .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private void applyRequest(Person p, PersonRequest req) {
        p.setFirstName(req.getFirstName());
        p.setLastName(req.getLastName());
        p.setEmail(req.getEmail());
        p.setPhone(req.getPhone());
        p.setWhatsapp(req.getWhatsapp());
        p.setBirthDate(req.getBirthDate());
        p.setNotes(req.getNotes());
        p.setLeadSource(req.getLeadSource());
        p.setAddress(req.getAddress());
        p.setClientId(req.getClientId());
        p.setAssignedToId(req.getAssignedToId());
        if (req.getStatus() != null) {
            try { p.setStatus(Person.PersonStatus.valueOf(req.getStatus())); } catch (IllegalArgumentException ignored) {}
        }
        if (req.getTags() != null) {
            p.setTags(req.getTags().toArray(new String[0]));
        }
    }

    private PersonResponse toResponse(Person p) {
        String clientName = null;
        if (p.getClientId() != null) {
            clientName = clientRepository.findById(p.getClientId())
                .map(c -> c.getName()).orElse(null);
        }
        String assignedToName = null;
        if (p.getAssignedToId() != null) {
            assignedToName = userRepository.findById(p.getAssignedToId())
                .map(u -> u.getFullName()).orElse(null);
        }
        return PersonResponse.builder()
            .id(p.getId())
            .tenantId(p.getTenantId())
            .firstName(p.getFirstName())
            .lastName(p.getLastName())
            .fullName(p.getFullName())
            .email(p.getEmail())
            .phone(p.getPhone())
            .whatsapp(p.getWhatsapp())
            .birthDate(p.getBirthDate())
            .notes(p.getNotes())
            .leadSource(p.getLeadSource())
            .status(p.getStatus() != null ? p.getStatus().name() : null)
            .assignedToId(p.getAssignedToId())
            .assignedToName(assignedToName)
            .clientId(p.getClientId())
            .clientName(clientName)
            .address(p.getAddress())
            .tags(p.getTags() != null ? Arrays.asList(p.getTags()) : List.of())
            .createdAt(p.getCreatedAt())
            .updatedAt(p.getUpdatedAt())
            .build();
    }
}
