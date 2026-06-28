package com.controltower.app.finance.domain;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class InvoiceSpecification {

    private InvoiceSpecification() {}

    public static Specification<Invoice> filter(
            UUID tenantId,
            Invoice.InvoiceStatus status,
            UUID clientId) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (clientId != null) {
                predicates.add(cb.equal(root.get("clientId"), clientId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
