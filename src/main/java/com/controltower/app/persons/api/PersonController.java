package com.controltower.app.persons.api;

import com.controltower.app.persons.api.dto.PersonRequest;
import com.controltower.app.persons.api.dto.PersonResponse;
import com.controltower.app.persons.application.PersonService;
import com.controltower.app.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/persons")
@RequiredArgsConstructor
@Tag(name = "Persons", description = "Individual CRM contacts — people (not companies)")
@SecurityRequirement(name = "bearerAuth")
public class PersonController {

    private final PersonService personService;

    @GetMapping
    @PreAuthorize("hasAuthority('client:read')")
    public ResponseEntity<ApiResponse<Page<PersonResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
            personService.list(search, PageRequest.of(page, size, Sort.by("firstName").ascending()))
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('client:read')")
    public ResponseEntity<ApiResponse<PersonResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(personService.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<PersonResponse>> create(@Valid @RequestBody PersonRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(personService.create(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<PersonResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody PersonRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(personService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('client:write')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        personService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/by-client/{clientId}")
    @PreAuthorize("hasAuthority('client:read')")
    public ResponseEntity<ApiResponse<List<PersonResponse>>> listByClient(@PathVariable UUID clientId) {
        return ResponseEntity.ok(ApiResponse.ok(personService.listByClient(clientId)));
    }
}
