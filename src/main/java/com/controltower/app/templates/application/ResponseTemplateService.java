package com.controltower.app.templates.application;

import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.templates.api.dto.ResponseTemplateRequest;
import com.controltower.app.templates.domain.ResponseTemplate;
import com.controltower.app.templates.domain.ResponseTemplateRepository;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResponseTemplateService {

    private final ResponseTemplateRepository repo;

    @Transactional(readOnly = true)
    public Page<ResponseTemplate> list(String q, String category, Pageable pageable) {
        UUID tenantId  = TenantContext.getTenantId();
        String qParam   = (q        != null && !q.isBlank())        ? q.trim()        : "";
        String catParam = (category != null && !category.isBlank()) ? category.trim() : null;
        return repo.findFiltered(tenantId, catParam, qParam, pageable);
    }

    @Transactional
    public ResponseTemplate create(ResponseTemplateRequest req, UUID authorId) {
        ResponseTemplate t = new ResponseTemplate();
        t.setTenantId(TenantContext.getTenantId());
        t.setAuthorId(authorId);
        apply(t, req);
        return repo.save(t);
    }

    @Transactional
    public ResponseTemplate update(UUID id, ResponseTemplateRequest req) {
        ResponseTemplate t = resolve(id);
        apply(t, req);
        return repo.save(t);
    }

    @Transactional
    public void delete(UUID id) {
        ResponseTemplate t = resolve(id);
        t.softDelete();
        repo.save(t);
    }

    private void apply(ResponseTemplate t, ResponseTemplateRequest req) {
        t.setName(req.getName());
        t.setBody(req.getBody());
        t.setCategory(req.getCategory());
        t.setShortcut(req.getShortcut());
    }

    private ResponseTemplate resolve(UUID id) {
        return repo.findByIdAndTenantIdAndDeletedAtIsNull(id, TenantContext.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException("ResponseTemplate", id));
    }
}
