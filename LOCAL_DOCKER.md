# Running Control Tower Locally with Docker

## Prerequisites

| Tool | Version | Install |
|------|---------|---------|
| Docker Desktop | ≥ 4.x | https://docs.docker.com/get-docker/ |
| Docker Compose | ≥ 2.x | bundled with Docker Desktop |
| Git | any | https://git-scm.com |

> Java and Gradle do **not** need to be installed locally — the build happens inside Docker.

---

## 1. Clone the repository

```bash
git clone https://github.com/kalg12/ControlTower-App-Backend.git
cd ControlTower-App-Backend
```

---

## 2. Create your `.env` file

```bash
cp .env.example .env
```

The defaults work for local development out of the box. The only values you may want to change:

```env
# Required for Stripe integration (leave empty to skip)
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...

# Use stronger secrets in any shared environment
JWT_SECRET=change-me-to-a-strong-256-bit-secret-key-in-production
ENCRYPTION_KEY=0123456789abcdef0123456789abcdef   # must be exactly 32 chars

# Email (optional — password reset, welcome emails)
# Use Mailtrap (https://mailtrap.io) for local testing
MAIL_HOST=smtp.mailtrap.io
MAIL_PORT=587
MAIL_USERNAME=your_mailtrap_user
MAIL_PASSWORD=your_mailtrap_pass
```

> **Important:** `.env` is git-ignored. Never commit it.

---

## 3. Start everything

```bash
docker compose up --build
```

This will:
1. Build the app JAR inside a Docker container (takes ~2-3 min on first run)
2. Start PostgreSQL 17, Redis 7, and the app
3. Flyway runs all 12 migrations automatically on startup
4. App becomes available at **http://localhost:8080**

### First run output you should see

```
controltower-postgres  | database system is ready to accept connections
controltower-redis     | Ready to accept connections
controltower-app       | Flyway: Successfully applied 12 migrations
controltower-app       | Started ControltowerAppApplication in X.XXX seconds
```

---

## 4. Verify the app is running

```bash
# Health check
curl http://localhost:8080/actuator/health

# Expected response
{"status":"UP"}
```

Open Swagger UI: **http://localhost:8080/swagger-ui/index.html**

---

## 5. Create your first tenant and log in

Use the public onboarding endpoint — no SQL required:

```bash
curl -s -X POST http://localhost:8080/api/v1/tenants/onboard \
  -H "Content-Type: application/json" \
  -d '{
    "tenantName": "Demo Tenant",
    "tenantSlug": "demo",
    "adminEmail": "admin@demo.com",
    "adminPassword": "Admin123!",
    "adminFullName": "Admin User"
  }' | jq .
```

This creates the tenant, admin user (with all permissions), and a Trial license in one call.

```bash
# Log in
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@demo.com","password":"Admin123!"}' | jq .
```

Copy the `accessToken` from the response. In Swagger UI, click **Authorize** and paste it.

---

## 6. Common commands

```bash
# Start in background (detached)
docker compose up -d

# View app logs (live)
docker compose logs -f app

# View all logs
docker compose logs -f

# Stop everything (keeps data)
docker compose down

# Stop and wipe all data (fresh start)
docker compose down -v

# Rebuild only the app after a code change
docker compose up --build app

# Open a Postgres shell
docker exec -it controltower-postgres psql -U controltower -d controltower

# Open a Redis shell
docker exec -it controltower-redis redis-cli
```

---

## 7. Running only the infrastructure (dev mode)

If you want to run the app locally with Gradle (hot reload) and only use Docker for the databases:

```bash
# Start only postgres + redis
docker compose up postgres redis -d

# Run the app locally
./gradlew bootRun
```

The app connects to `localhost:5432` and `localhost:6379` by default (`.env` defaults).

---

## 8. Port reference

| Service | Default port | Override env var |
|---------|-------------|-----------------|
| App | 8080 | `SERVER_PORT` |
| PostgreSQL | 5432 | `DB_PORT` |
| Redis | 6379 | `REDIS_PORT` |

To avoid port conflicts, change the values in your `.env`:

```env
SERVER_PORT=9090
DB_PORT=5433
REDIS_PORT=6380
```

---

## 9. Flyway migrations

Migrations live in `src/main/resources/db/migration/` and run automatically on startup.

| Migration | Description |
|-----------|-------------|
| V1 | pgcrypto extension |
| V2 | Tenants, users, roles, permissions |
| V3 | Tenant configs |
| V4 | Clients and branches |
| V5 | Audit logs |
| V6 | Health monitoring |
| V7 | Support tickets + SLA |
| V8 | Licenses and plans |
| V9 | Notifications |
| V10 | Kanban boards + notes |
| V11 | Integrations |
| V12 | Billing / Stripe |
| V13 | Password reset tokens |
| V14 | Ticket attachments + health snapshots |
| V15 | 2FA columns on users |

To check migration status:

```bash
docker exec -it controltower-postgres psql -U controltower -d controltower \
  -c "SELECT version, description, installed_on, success FROM flyway_schema_history ORDER BY installed_rank;"
```

---

## 10. Troubleshooting

**App fails to start — `Unable to acquire JDBC Connection`**
→ Postgres is still initializing. Wait 15–20 seconds and run `docker compose restart app`.

**Port already in use**
→ Change the port in `.env` (see section 8).

**401 on login after onboarding**
→ Make sure you used the correct `adminPassword` value in the onboard request.
→ Passwords are BCrypt-hashed with strength 12. If you need to reset a password, call `POST /auth/forgot-password` (requires email to be configured) or re-run onboarding with a new slug.

**Flyway `checksumMismatch` error**
→ A migration file was modified after it was applied. Either restore the original file or wipe the DB:
```bash
docker compose down -v && docker compose up --build
```

**Redis `WRONGPASS` error**
→ `REDIS_PASSWORD` in `.env` does not match what Redis was started with. Run `docker compose down -v` to reset.
