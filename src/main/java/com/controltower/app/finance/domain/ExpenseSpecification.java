package com.controltower.app.finance.domain;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ExpenseSpecification {

    private ExpenseSpecification() {}

    public static Specification<Expense> filter(
            UUID tenantId,
            Expense.ExpenseCategory category,
            UUID clientId,
            String vendor,
            BigDecimal amountMin,
            BigDecimal amountMax,
            Instant from,
            Instant to) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }
            if (clientId != null) {
                predicates.add(cb.equal(root.get("clientId"), clientId));
            }
            if (vendor != null && !vendor.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("vendor")),
                        "%" + vendor.toLowerCase() + "%"));
            }
            if (amountMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), amountMin));
            }
            if (amountMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), amountMax));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("paidAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("paidAt"), to));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
