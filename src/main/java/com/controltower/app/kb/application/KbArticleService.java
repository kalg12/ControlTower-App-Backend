package com.controltower.app.kb.application;

import com.controltower.app.kb.api.dto.KbArticleRequest;
import com.controltower.app.kb.api.dto.KbArticleResponse;
import com.controltower.app.kb.domain.KbArticle;
import com.controltower.app.kb.domain.KbArticleRepository;
import com.controltower.app.kb.domain.KbStatus;
import com.controltower.app.shared.exception.ResourceNotFoundException;
import com.controltower.app.tenancy.domain.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KbArticleService {

    private final KbArticleRepository repo;

    @Transactional(readOnly = true)
    public Page<KbArticleResponse> list(String q, KbStatus status, String category, Pageable pageable) {
        UUID tenantId = TenantContext.getTenantId();
        Page<KbArticle> page;
        if (q != null && !q.isBlank()) {
            page = repo.searchByText(tenantId, q.trim(), pageable);
        } else {
            page = repo.findFiltered(tenantId, status, category, pageable);
        }
        return page.map(KbArticleResponse::from);
    }

    @Transactional
    public KbArticleResponse getById(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        KbArticle article = resolve(id, tenantId);
        repo.incrementViews(id);
        return KbArticleResponse.from(article);
    }

    @Transactional
    public KbArticleResponse create(KbArticleRequest req, UUID authorId) {
        KbArticle article = new KbArticle();
        article.setTenantId(TenantContext.getTenantId());
        article.setAuthorId(authorId);
        applyRequest(article, req);
        return KbArticleResponse.from(repo.save(article));
    }

    @Transactional
    public KbArticleResponse update(UUID id, KbArticleRequest req) {
        KbArticle article = resolve(id, TenantContext.getTenantId());
        applyRequest(article, req);
        return KbArticleResponse.from(repo.save(article));
    }

    @Transactional
    public void delete(UUID id) {
        KbArticle article = resolve(id, TenantContext.getTenantId());
        article.softDelete();
        repo.save(article);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void applyRequest(KbArticle article, KbArticleRequest req) {
        article.setTitle(req.getTitle());
        article.setContent(req.getContent());
        article.setCategory(req.getCategory());
        if (req.getTags() != null) {
            article.setTagsCsv(String.join(",", req.getTags()));
        }
        if (req.getStatus() != null) {
            article.setStatus(req.getStatus());
        }
    }

    private KbArticle resolve(UUID id, UUID tenantId) {
        return repo.findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("KbArticle", id));
    }
}
