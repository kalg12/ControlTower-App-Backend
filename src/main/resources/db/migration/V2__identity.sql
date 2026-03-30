-- ─────────────────────────────────────────────────────────────────────────────
-- V2: Identity — Tenants, Users, Roles, Permissions
-- ─────────────────────────────────────────────────────────────────────────────

-- Tenants (Control Tower operator organizations)
CREATE TABLE tenants (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    status      VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_tenants_slug   ON tenants (slug)   WHERE deleted_at IS NULL;
CREATE INDEX idx_tenants_status ON tenants (status) WHERE deleted_at IS NULL;

-- Roles
CREATE TABLE roles (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID        REFERENCES tenants(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    code        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    is_system   BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, code)
);

CREATE INDEX idx_roles_tenant ON roles (tenant_id);

-- Permissions
CREATE TABLE permissions (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(150) NOT NULL UNIQUE,
    description VARCHAR(500),
    module      VARCHAR(100) NOT NULL
);

-- Role ↔ Permission join
CREATE TABLE role_permissions (
    role_id       UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- Users
CREATE TABLE users (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    status        VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    is_super_admin BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at    TIMESTAMP WITH TIME ZONE,
    UNIQUE (tenant_id, email)
);

CREATE INDEX idx_users_tenant        ON users (tenant_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_email         ON users (email)     WHERE deleted_at IS NULL;
CREATE INDEX idx_users_tenant_status ON users (tenant_id, status) WHERE deleted_at IS NULL;

-- User ↔ Role join
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- ── Seed: system-level permissions ────────────────────────────────────────────
INSERT INTO permissions (id, code, description, module) VALUES
    (gen_random_uuid(), 'tenant:read',         'View tenant details',        'tenancy'),
    (gen_random_uuid(), 'tenant:write',        'Create/edit tenants',        'tenancy'),
    (gen_random_uuid(), 'user:read',           'View users',                 'identity'),
    (gen_random_uuid(), 'user:write',          'Create/edit users',          'identity'),
    (gen_random_uuid(), 'client:read',         'View clients',               'clients'),
    (gen_random_uuid(), 'client:write',        'Create/edit clients',        'clients'),
    (gen_random_uuid(), 'health:read',         'View health status',         'health'),
    (gen_random_uuid(), 'health:write',        'Manage health rules',        'health'),
    (gen_random_uuid(), 'ticket:read',         'View tickets',               'support'),
    (gen_random_uuid(), 'ticket:write',        'Create/edit tickets',        'support'),
    (gen_random_uuid(), 'license:read',        'View licenses',              'licenses'),
    (gen_random_uuid(), 'license:write',       'Manage licenses',            'licenses'),
    (gen_random_uuid(), 'audit:read',          'View audit logs',            'audit'),
    (gen_random_uuid(), 'notification:read',   'View notifications',         'notifications'),
    (gen_random_uuid(), 'kanban:read',         'View boards and cards',      'kanban'),
    (gen_random_uuid(), 'kanban:write',        'Manage boards and cards',    'kanban'),
    (gen_random_uuid(), 'billing:read',        'View billing information',   'billing'),
    (gen_random_uuid(), 'billing:write',       'Manage billing',             'billing'),
    (gen_random_uuid(), 'integration:read',    'View integrations',          'integrations'),
    (gen_random_uuid(), 'integration:write',   'Manage integrations',        'integrations');
