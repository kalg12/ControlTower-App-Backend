# Control Tower — API Endpoints Reference

Base URL: `http://localhost:8080/api/v1`
Swagger UI: `http://localhost:8080/swagger-ui/index.html`
OpenAPI JSON: `http://localhost:8080/v3/api-docs`

> **Auth:** All protected endpoints require `Authorization: Bearer <access_token>`.
> Get a token via `POST /auth/login`.

---

## Auth

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/auth/login` | Public | Login with email + password. Returns `accessToken` + `refreshToken` (or `mfaToken` if 2FA enabled). |
| POST | `/auth/refresh` | Public | Rotate token pair using a valid refresh token. |
| POST | `/auth/logout` | Public | Invalidate refresh token (server-side). |
| POST | `/auth/forgot-password` | Public | Request password reset email. Always returns 200 (no user enumeration). |
| POST | `/auth/reset-password` | Public | Reset password using token from email. Body: `{ token, newPassword }`. |
| POST | `/auth/2fa/setup` | Bearer | Generate TOTP secret + QR URL (does not enable 2FA yet). |
| POST | `/auth/2fa/enable` | Bearer | Confirm TOTP setup by verifying first code. Body: `{ code }`. |
| POST | `/auth/2fa/disable` | Bearer | Disable 2FA (requires valid TOTP code). Body: `{ code }`. |
| POST | `/auth/2fa/verify` | Public | Exchange `mfaToken` + TOTP code for full tokens. Body: `{ mfaToken, code }`. |

**2FA login flow:**
```
POST /auth/login  →  { requiresMfa: true, mfaToken: "..." }
POST /auth/2fa/verify  →  { accessToken: "...", refreshToken: "..." }
```

---

## Tenant Onboarding *(public — first-time setup)*

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/tenants/onboard` | **Public** | Create tenant + admin user + admin role + trial license in one transaction. |

Request body:
```json
{
  "tenantName": "Acme Corp",
  "tenantSlug": "acme",
  "adminEmail": "admin@acme.com",
  "adminPassword": "Admin123!",
  "adminFullName": "Admin User"
}
```

---

## Users

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/users` | `user:read` | List users (paginated, filterable by status). |
| POST | `/users` | `user:write` | Create a new user within the current tenant. |
| GET | `/users/{id}` | `user:read` | Get user by ID. |
| PUT | `/users/{id}` | `user:write` | Update user. |
| DELETE | `/users/{id}` | `user:write` | Soft-delete user. |
| POST | `/users/{id}/roles/{roleId}` | `user:write` | Assign a role to a user. |
| DELETE | `/users/{id}/roles/{roleId}` | `user:write` | Remove a role from a user. |

---

## Roles & Permissions

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/roles` | `user:read` | List roles for current tenant (paginated). |
| POST | `/roles` | `user:write` | Create a custom role. |
| DELETE | `/roles/{id}` | `user:write` | Delete role (system roles are protected). |
| POST | `/roles/{id}/permissions/{permissionId}` | `user:write` | Add a permission to a role. |
| DELETE | `/roles/{id}/permissions/{permissionId}` | `user:write` | Remove a permission from a role. |
| GET | `/permissions` | `user:read` | List all available system permissions. |

---

## Tenants *(super-admin only)*

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/tenants` | `tenant:read` | List all tenants (paginated). |
| POST | `/tenants` | `tenant:write` | Create a new tenant. |
| GET | `/tenants/{id}` | `tenant:read` | Get tenant by ID. |
| PUT | `/tenants/{id}` | `tenant:write` | Update tenant. |
| POST | `/tenants/{id}/suspend` | `tenant:write` | Suspend a tenant. |
| POST | `/tenants/{id}/reactivate` | `tenant:write` | Reactivate a suspended tenant. |
| GET | `/tenants/{id}/config` | `tenant:read` | Get tenant config key-value pairs. |
| PUT | `/tenants/{id}/config/{key}` | `tenant:write` | Set a tenant config value. |

---

## Clients

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/clients` | `client:read` | List clients (paginated, searchable by name). |
| POST | `/clients` | `client:write` | Create a client. |
| GET | `/clients/{id}` | `client:read` | Get client by ID. |
| PUT | `/clients/{id}` | `client:write` | Update client. |
| DELETE | `/clients/{id}` | `client:write` | Soft-delete client. |
| GET | `/clients/{id}/branches` | `client:read` | List branches for a client. |
| POST | `/clients/{id}/branches` | `client:write` | Create a branch for a client. |
| PUT | `/clients/{id}/branches/{branchId}` | `client:write` | Update a branch. |
| DELETE | `/clients/{id}/branches/{branchId}` | `client:write` | Soft-delete a branch. |

---

## Health Monitoring

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/health/heartbeat/{branchSlug}` | **Public** | Receive heartbeat ping from a client system. |
| GET | `/health/clients` | `health:read` | Overview of all clients and their health status. |
| GET | `/health/branches/{branchId}` | `health:read` | Paginated health checks for a specific branch. |
| GET | `/health/incidents` | `health:read` | Paginated list of health incidents. |
| POST | `/health/incidents/{id}/resolve` | `health:write` | Manually resolve an incident. |
| POST | `/health/rules` | `health:write` | Create a health rule (threshold, severity, channel). |

---

## Support / Tickets

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/tickets` | `ticket:read` | List tickets. Filters: `status`, `assigneeId`, `clientId`, `priority`, `createdAfter`, `createdBefore`. Use `slaAtRisk=true` to show tickets expiring within `slaWindowHours` (default 4). |
| POST | `/tickets` | `ticket:write` | Create a ticket manually. |
| GET | `/tickets/{id}` | `ticket:read` | Get ticket by ID. |
| GET | `/tickets/export` | `ticket:read` | Download all matching tickets as a CSV file. Accepts same filter params as GET /tickets. |
| PATCH | `/tickets/{id}/status` | `ticket:write` | Transition ticket status (state machine validated). |
| POST | `/tickets/{id}/assign` | `ticket:write` | Assign ticket to a user (`?assigneeId=`). |
| POST | `/tickets/{id}/escalate` | `ticket:write` | Escalate ticket priority by one level. |
| POST | `/tickets/{id}/comments` | `ticket:write` | Add a comment (set `internal: true` for agent-only notes). |
| DELETE | `/tickets/{id}` | `ticket:write` | Soft-close a ticket. |
| POST | `/tickets/bulk/status` | `ticket:write` | Update status of multiple tickets at once. Body: `{ ticketIds: [...], status: "RESOLVED" }`. |
| POST | `/tickets/bulk/assign` | `ticket:write` | Assign multiple tickets to one user. Body: `{ ticketIds: [...], assigneeId: "uuid" }`. |
| POST | `/tickets/{id}/attachments` | `ticket:write` | Upload file attachment (`multipart/form-data`, field: `file`). |
| GET | `/tickets/{id}/attachments` | `ticket:read` | List all attachments for a ticket. |
| GET | `/attachments/{attachmentId}` | `ticket:read` | Download a file attachment. |
| DELETE | `/attachments/{attachmentId}` | `ticket:write` | Delete an attachment. |

**Advanced filters for GET /tickets:**
| Param | Type | Description |
|-------|------|-------------|
| `status` | enum | OPEN, IN_PROGRESS, WAITING, RESOLVED, CLOSED |
| `priority` | enum | LOW, MEDIUM, HIGH, CRITICAL |
| `assigneeId` | UUID | Filter by assignee |
| `clientId` | UUID | Filter by client |
| `createdAfter` | ISO-8601 | e.g. `2026-01-01T00:00:00Z` |
| `createdBefore` | ISO-8601 | e.g. `2026-12-31T23:59:59Z` |
| `slaAtRisk` | boolean | Show only tickets whose SLA expires soon |
| `slaWindowHours` | int | Hours ahead to consider "at risk" (default 4) |

**Status transitions:**
```
OPEN → IN_PROGRESS | RESOLVED | CLOSED
IN_PROGRESS → WAITING | RESOLVED | CLOSED
WAITING → IN_PROGRESS | RESOLVED
RESOLVED → CLOSED | OPEN
CLOSED → OPEN
```

**SLA windows by priority:**
| Priority | Window |
|----------|--------|
| LOW | 48 h |
| MEDIUM | 24 h |
| HIGH | 8 h |
| CRITICAL | 2 h |

---

## Audit Log

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/audit` | `audit:read` | Paginated audit log (filter by `action`, `userId`, `resourceType`, date range). |

---

## Licenses

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/licenses` | `license:read` | List licenses for current tenant (paginated). |
| POST | `/licenses` | `license:write` | Activate a license for a client (assign plan, optional trial days). |
| GET | `/licenses/{id}` | `license:read` | Get license by ID. |
| GET | `/licenses/clients/{clientId}` | `license:read` | Get license by client ID. |
| POST | `/licenses/{id}/suspend` | `license:write` | Suspend a license. |
| POST | `/licenses/{id}/reactivate` | `license:write` | Reactivate with `?extensionDays=30`. |
| GET | `/licenses/{id}/features` | `license:read` | List enabled feature codes for a license. |
| GET | `/licenses/plans` | `license:read` | List all active plans (catalog). |

**License status flow:**
```
TRIAL → ACTIVE → GRACE → SUSPENDED → CANCELLED
                          ↑ payment failure (7-day grace)
```

---

## Notifications

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/notifications` | `notification:read` | List notifications for the current user (paginated). |
| PATCH | `/notifications/{id}/read` | `notification:read` | Mark a notification as read. |
| PATCH | `/notifications/read-all` | `notification:read` | Mark all notifications as read. |
| DELETE | `/notifications/{id}` | `notification:read` | Delete a notification for the current user. |

**WebSocket (STOMP):** Connect to `/ws` (SockJS), subscribe to `/user/queue/notifications`.

---

## Kanban Boards

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/boards` | `kanban:read` | List boards for current tenant (paginated). |
| POST | `/boards` | `kanban:write` | Create a board (`PRIVATE` or `TEAM` visibility). |
| GET | `/boards/{id}` | `kanban:read` | Get board with columns and cards. |
| PUT | `/boards/{id}` | `kanban:write` | Update board name/description/visibility. |
| DELETE | `/boards/{id}` | `kanban:write` | Soft-delete board. |
| POST | `/boards/{id}/columns` | `kanban:write` | Add a column (`?name=&position=`). |
| DELETE | `/boards/columns/{columnId}` | `kanban:write` | Delete a column (cascades cards). |
| POST | `/boards/cards` | `kanban:write` | Create a card in a column. |
| PATCH | `/boards/cards/{cardId}/move` | `kanban:write` | Move card to another column with new position. |
| DELETE | `/boards/cards/{cardId}` | `kanban:write` | Soft-delete a card. |
| POST | `/boards/cards/{cardId}/checklist` | `kanban:write` | Add checklist item (`?text=`). |
| PATCH | `/boards/checklist/{itemId}/toggle` | `kanban:write` | Toggle checklist item completed/incomplete. |

---

## Notes

| Method | Path | Auth | Any authenticated user |
|--------|------|------|------------------------|
| GET | `/notes` | Authenticated | List notes (filter by `?linkedTo=CLIENT&linkedId=<uuid>`). |
| POST | `/notes` | Authenticated | Create a note (optionally linked to `CLIENT`, `TICKET`, or `BRANCH`). |
| GET | `/notes/{id}` | Authenticated | Get note by ID. |
| PUT | `/notes/{id}` | Authenticated | Update note title/content. |
| DELETE | `/notes/{id}` | Authenticated | Soft-delete note. |

---

## Integrations

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/integrations` | `integration:read` | List integration endpoints (paginated). |
| POST | `/integrations` | `integration:write` | Register a new endpoint (`POS` or `CUSTOM` type). |
| PUT | `/integrations/{id}` | `integration:write` | Update endpoint config. |
| DELETE | `/integrations/{id}` | `integration:write` | Deactivate an endpoint. |
| POST | `/integrations/events` | **Public** (X-Api-Key) | Receive a push event from an external system. |

**Header for push events:** `X-Api-Key: <configured_api_key>`

> API keys are stored AES-256-GCM encrypted. Pass the plain-text key when registering; the system encrypts it automatically.

---

## Dashboard

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/dashboard` | Authenticated | Aggregated stats: clients, branches, health status, tickets, licenses, unread notifications for current tenant. |

Response fields: `totalClients`, `activeBranches`, `branchesUp`, `branchesDown`, `branchesDegraded`, `openIncidents`, `openTickets`, `ticketsInProgress`, `slaBreachedTickets`, `activeLicenses`, `trialLicenses`, `expiredLicenses`, `unreadNotifications`.

---

## Billing

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/billing/events` | `billing:read` | Paginated billing event history for current tenant. |
| POST | `/billing/stripe/webhook` | **Public** (Stripe-Signature) | Stripe webhook receiver (idempotent). |

**Handled Stripe event types:**
- `customer.subscription.created` / `updated` → activates license
- `customer.subscription.deleted` → cancels license
- `invoice.paid` → records payment event
- `invoice.payment_failed` → enters 7-day grace period

---

## Infrastructure Behaviour

### Rate Limiting
Public endpoints are rate-limited at **60 requests / minute per IP**:
- `POST /health/heartbeat/**`
- `POST /integrations/events`
- `POST /billing/stripe/webhook`

Exceeding the limit returns HTTP **429** with `{ "success": false, "message": "Rate limit exceeded" }`.

### File Attachments (local storage)
Uploaded files are stored under the path configured in `STORAGE_PATH` (default `./uploads`).
Upload using `multipart/form-data` with field name `file`.
Download returns the file with the original `Content-Type` header.

### Background Jobs
| Job | Schedule | Description |
|-----|----------|-------------|
| SLA breach checker | Every 5 min | Marks tickets whose SLA `due_at` has passed |
| Webhook retry | Every 5 min | Retries `PENDING` webhook deliveries (max 3 attempts) |
| Health pull check | Every 5 min | Pulls health status from endpoints with a `pullUrl` |
| Health daily snapshot | Daily 01:00 UTC | Aggregates uptime%, avg latency, incident count per branch |
| License expiry check | Daily 02:00 UTC | Moves expired TRIAL/ACTIVE licenses to GRACE period |
| License grace expiry | Daily 02:05 UTC | Suspends licenses whose grace period has ended |

### Email Notifications
Configure SMTP via `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` in `.env`.
If mail is not configured, emails are logged as warnings and skipped — the app continues normally.

---

## Pagination

All `GET` list endpoints accept:

| Param | Default | Description |
|-------|---------|-------------|
| `page` | `0` | Zero-based page index |
| `size` | `20` | Items per page (max varies per endpoint) |

Response envelope:
```json
{
  "success": true,
  "data": {
    "content": [...],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "last": false
  }
}
```

---

## Standard Response Envelope

```json
{
  "success": true,
  "message": "Optional message",
  "data": { ... },
  "timestamp": "2026-03-31T10:00:00Z"
}
```

Error response:
```json
{
  "success": false,
  "message": "Error description",
  "errors": ["field: validation message"],
  "timestamp": "2026-03-31T10:00:00Z"
}
```
