package com.controltower.app.licenses.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "plan_features",
       uniqueConstraints = @UniqueConstraint(columnNames = {"plan_id", "feature_code"}))
@Getter
@Setter
public class PlanFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @Column(name = "feature_code", nullable = false)
    private String featureCode;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "limit_value")
    private Integer limitValue;
}
