# Testing Guide — Control Tower Backend

This document explains how to run the integration test suite locally.

---

## Prerequisites

| Tool | Minimum version | Notes |
|------|----------------|-------|
| Docker Desktop | 4.x | **Required** — Testcontainers starts PostgreSQL and Redis inside Docker automatically |
| Java 21 | 21 | OR just use Gradle wrapper (downloads JDK automatically via toolchain) |
| Gradle | 9.x | Use the `./gradlew` wrapper — no global install needed |

> **Docker must be running** before you execute any test command. Testcontainers pulls `postgres:17-alpine` and `redis:7-alpine` images on first run.

---

## How the tests work

All integration tests extend `BaseIntegrationTest`, which:

1. Starts a **PostgreSQL 17** container and a **Redis 7** container via Testcontainers.
2. Injects the dynamic ports into the Spring `ApplicationContext` via `@DynamicPropertySource`.
3. Runs all **Flyway migrations** against the test database automatically on context startup.
4. Tears down the containers after the test suite completes.

No external database or Redis instance is needed. Email (SMTP) and Stripe are disabled for tests.

---

## Running all tests

```bash
./gradlew test
```

This command:
- Starts Docker containers (pulled automatically on first run; ~30 s)
- Runs the full Spring context (Flyway, JPA, Security, WebSocket)
- Executes all `@Test` methods across all test classes
- Generates reports in `build/reports/tests/test/index.html`

---

## Running a specific test class

```bash
./gradlew test --tests "com.controltower.app.auth.AuthIntegrationTest"
```

Other class names:

| Class | What it covers |
|-------|---------------|
| `ControltowerAppApplicationTests` | Context loads (smoke test) |
| `AuthIntegrationTest` | Login, refresh, logout, password reset, 401/403 flows |
| `TenantIsolationTest` | Tenant A data invisible to Tenant B |
| `TicketStateMachineTest` | Valid/invalid state transitions on tickets |
| `HealthHeartbeatIntegrationTest` | Heartbeat endpoint, incident auto-creation |
| `LicenseLifecycleTest` | Trial → suspend → reactivate |
| `RateLimitIntegrationTest` | 429 after exceeding 60 req/min on public endpoints |
| `SecurityHeadersTest` | HSTS, CSP, X-Frame-Options, Correlation-ID in responses |
| `DashboardStatsTest` | `/dashboard` returns correct aggregated counts |

---

## Running a single test method

```bash
./gradlew test --tests "com.controltower.app.auth.AuthIntegrationTest.login_validCredentials_returnsTokens"
```

---

## Viewing the HTML report

After the tests finish:

```bash
open build/reports/tests/test/index.html   # macOS
# or
xdg-open build/reports/tests/test/index.html  # Linux
```

---

## First-run tips

- **Docker image pull**: On the first run Testcontainers downloads `postgres:17-alpine` and `redis:7-alpine`. Expect ~30–60 s of extra setup time.
- **Context startup**: The full Spring Boot context with Flyway migrations takes ~10–20 s on first run; subsequent runs reuse the JVM and are faster.
- **Parallel execution**: Tests annotated with `@DirtiesContext` reset the Spring context between runs. For speed, run individual test classes in parallel (Gradle handles this automatically).

---

## Disabling specific tests

Annotate the class or method with `@Disabled("reason")`:

```java
@Test
@Disabled("Flaky in CI — see issue #42")
void someTest() { ... }
```

---

## CI/CD

The GitHub Actions workflow (`.github/workflows/ci.yml`) runs the full test suite on every push and pull request to `master`. It uses Docker-in-Docker (`ubuntu-latest` runner includes Docker) so Testcontainers works without any special configuration.

The pipeline:
1. Compiles the project
2. Runs all integration tests
3. Publishes JUnit XML test results (visible in the PR checks tab)
4. Builds the Docker image
5. Runs a Trivy vulnerability scan (non-blocking, report only)

---

## Environment variables for tests

No `.env` file is needed. All test values are hardcoded in `BaseIntegrationTest`:

| Variable | Test value | Source |
|----------|-----------|--------|
| `DB_HOST` / `DB_PORT` | Dynamic from Testcontainers | `@DynamicPropertySource` |
| `REDIS_HOST` / `REDIS_PORT` | Dynamic from Testcontainers | `@DynamicPropertySource` |
| `MAIL_HOST` | empty (email skipped) | `@DynamicPropertySource` |
| `JWT_SECRET` | dev default from `application.yml` | property file |
| `ENCRYPTION_KEY` | dev default from `application.yml` | property file |
| `STRIPE_SECRET_KEY` | empty (webhook tests skipped) | `@DynamicPropertySource` |

---

## Troubleshooting

**`Cannot connect to the Docker daemon`**
→ Make sure Docker Desktop is running before executing `./gradlew test`.

**`Could not pull image postgres:17-alpine`**
→ Check your internet connection. The image is only pulled once; subsequent runs use the local cache.

**`FlywayException: Validate failed`**
→ A migration file was modified. Wipe the test container's volume by running:
```bash
docker system prune -f
./gradlew test
```
Testcontainers creates a fresh database on every test run, so this is only relevant if you modified a migration between test runs and a container was cached.

**`Port already in use`**
→ Testcontainers uses random ports — this shouldn't happen. If it does, restart Docker.

**Out of memory during tests**
→ Increase JVM heap in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx3g
```

**Tests pass locally but fail in CI**
→ Check Docker daemon availability in CI. The CI workflow uses `ubuntu-latest` which includes Docker. If using a self-hosted runner, ensure Docker is installed and the runner user has access to the Docker socket.
