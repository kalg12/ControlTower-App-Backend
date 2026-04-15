# Control Tower API — Endpoint Reference

Base URL: `https://api.controltower.app/api/v1`  
Auth: `Authorization: Bearer <jwt>` on all endpoints.

---

## Time Tracking — `/api/v1/time-entries`

| Method | Path | Permission | Description |
|--------|------|------------|-------------|
| POST | `/time-entries/start` | tickets:write \| kanban:write | Start a timer for a ticket or card. Stops any running timer first. |
| PATCH | `/time-entries/{id}/stop` | tickets:write \| kanban:write | Stop a running timer. Computes elapsed minutes. |
| GET | `/time-entries/active` | tickets:read \| kanban:read | Get current user's active (running) timer. |
| POST | `/time-entries/log` | tickets:write \| kanban:write | Manually log time (minutes + note). |
| GET | `/time-entries?entityType=&entityId=` | tickets:read \| kanban:read | List all time entries for a ticket or card. |
| GET | `/time-entries/summary?entityType=&entityId=` | tickets:read \| kanban:read | Estimated vs logged time summary. |
| DELETE | `/time-entries/{id}` | tickets:write \| kanban:write | Soft-delete a time entry. |

### Start Timer Request
```json
{ "entityType": "TICKET", "entityId": "uuid" }
```

### Log Time Request
```json
{ "entityType": "CARD", "entityId": "uuid", "minutes": 45, "note": "Fixed the bug" }
```

---

## SLA Configuration — `/api/v1/sla-config`

| Method | Path | Permission | Description |
|--------|------|------------|-------------|
| GET | `/sla-config` | tickets:read \| settings:read | Get all SLA windows (hours) for the current tenant. |
| PUT | `/sla-config` | settings:write | Update SLA windows. Partial updates supported. |

### SLA Config Response
```json
{ "low": 48, "medium": 24, "high": 8, "critical": 2 }
```

### Update SLA Config Request (all fields optional)
```json
{ "low": 72, "medium": 48, "high": 12, "critical": 4 }
```

---

## Time Analytics — `/api/v1/analytics/time`

| Method | Path | Permission | Description |
|--------|------|------------|-------------|
| GET | `/analytics/time?from=&to=` | tickets:read | Aggregate time metrics for the tenant. Defaults to last 30 days. |

### Response
```json
{
  "avgResolutionMinutes": 142.5,
  "slaComplianceRate": 87.5,
  "totalEntries": 48,
  "totalLoggedMinutes": 2340,
  "topUsers": [
    { "userId": "uuid", "totalMinutes": 480 }
  ]
}
```

---

## Tickets — `/api/v1/tickets` (updated fields)

`TicketResponse` now includes:
- `estimatedMinutes` (Integer, nullable)
- `slaDueAt` (Instant, nullable)
- `slaBreached` (Boolean, nullable)

`CreateTicketRequest` now accepts:
- `estimatedMinutes` (Integer, nullable)

---

## Kanban Cards — `/api/v1/boards/cards` (updated fields)

`CardResponse` now includes:
- `estimatedMinutes` (Integer, nullable)

`CardRequest` and `CardUpdateRequest` now accept:
- `estimatedMinutes` (Integer, nullable)
