package com.controltower.app.kanban.domain;

import com.controltower.app.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "boards")
@Getter
@Setter
public class Board extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    private Visibility visibility = Visibility.TEAM;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "client_id")
    private UUID clientId;

    /** Not named "columns" — can clash with JPQL/SQL reserved words in fetch queries. */
    @OneToMany(mappedBy = "board", cascade = CascadeType.ALL, fetch = FetchType.LAZY,
               orphanRemoval = true)
    @OrderBy("position ASC")
    private List<BoardColumn> boardColumns = new ArrayList<>();

    public enum Visibility { PRIVATE, TEAM }
}
