package com.controltower.app.finance.domain;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PurchaseRecordSpecification {

    private PurchaseRecordSpecification() {}

    public static Specification<PurchaseRecord> filter(
            UUID tenantId,
            PurchaseRecord.PurchaseSource source,
            Expense.ExpenseCategory category,
            String vendor,
            Instant from,
            Instant to) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (source != null) {
                predicates.add(cb.equal(root.get("source"), source));
            }
            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (vendor != null && !vendor.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("vendor")),
                        "%" + vendor.toLowerCase() + "%"));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("purchasedAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("purchasedAt"), to));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
