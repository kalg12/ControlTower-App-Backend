package com.controltower.app.email.domain;

import com.controltower.app.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Array;

import java.util.UUID;

@Entity
@Table(name = "email_aliases")
@Getter
@Setter
public class EmailAlias extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "mailbox_id", nullable = false)
    private UUID mailboxId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "alias", nullable = false)
    private String alias;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Array(length = 20)
    @Column(name = "forward_to", columnDefinition = "TEXT[]")
    private String[] forwardTo = new String[0];
}
