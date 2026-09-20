# Phase 1 — Java Backend Foundation Report

Date: 2026-09-20

This report distinguishes implementation/build evidence from runtime evidence. The Java build, unit tests, architecture tests, formatting check, static architecture validation, and Compose configuration validation passed locally. The local Docker engine was unavailable, so the six Testcontainers scenarios, container startup, Flyway against MySQL 8.4, application health, and HTTP smoke flow were not executed on this machine. The completion flags remain false where those runtime checks are required.

## 1. Implemented Scope

Phase 1 implements a Java 21 / Spring Boot 4.1.1 modular monolith foundation for identity and access management. It includes email-based login, JWT access tokens, rotating opaque refresh tokens, logout/revocation, permission-based RBAC, minimum administrator user APIs, unified errors, request correlation, audit events, bootstrap administrator creation, health/OpenAPI endpoints, Flyway, Docker packaging, automated tests, and CI.

There is no public registration endpoint because `docs/PRODUCT_SCOPE.md` requires administrator-provisioned accounts. Bootstrap creation and the narrow administrator API provide the Phase 1 account-creation path.

## 2. Files / Modules Created

- `backend-java/pom.xml`, Maven Wrapper, Java 21 Spring Boot application and configuration profiles.
- `auth`: login, JWT, refresh rotation/replay handling, logout, CSRF/origin checks, session registry, Spring Security handlers and bootstrap runner.
- `user`: User, Department, Role, Permission, repositories, application service, DTOs and minimum admin/current-user endpoints.
- `audit`: append-only entity/repository, allowlisted write service, query service and protected read endpoint.
- `shared`: error envelope/handler, request ID filter, health, clock and OpenAPI configuration.
- `ticket`, `knowledge`, `ai`: package documentation only.
- `backend-java/Dockerfile`, runtime healthcheck, root Compose integration and `.env.example` values.
- `.github/workflows/java.yml`, unit/architecture/integration test sources and Spotless configuration.
- `docs/PHASE_1_JAVA_FOUNDATION.md` and `docs/CONTRACT_DEVIATION.md`.

## 3. Database Tables Added

`V1__identity_auth_audit.sql` contains eight Phase 1 tables:

1. `departments`
2. `users`
3. `roles`
4. `permissions`
5. `user_roles`
6. `role_permissions`
7. `refresh_tokens`
8. `audit_logs`

The migration includes primary/foreign keys, unique constraints for email/role/permission/token hash, status checks, refresh and audit indexes, timestamps, the `GENERAL` department, four fixed roles, Phase 1 permissions, and future permission codes only. Hibernate uses `ddl-auto=validate`; no later-phase tables were added.

## 4. Authentication

`POST /api/v1/auth/login` validates email/password and active user state. Passwords use BCrypt cost 12, accept 12–72 characters, and are never returned or logged. `GET /api/v1/users/me` returns the authenticated DTO. Administrator-only user creation replaces public registration.

The security chain includes the JWT filter, REST `AuthenticationEntryPoint`, REST `AccessDeniedHandler`, method security and stateless request handling.

## 5. JWT / Refresh

Access JWTs default to ten minutes and contain `iss`, `sub`, `roles`, `iat`, `exp`, `jti`, and `sid`. Signature, issuer and timestamp validation use the configured secret and injected clock. Authorization reloads the current user, roles and permissions from MySQL rather than trusting stale role claims.

Refresh tokens are random 256-bit opaque values; MySQL stores only SHA-256 hashes. Rotation consumes the old token and creates a replacement in the same family. Reuse revokes the family. Refresh/logout use an HttpOnly refresh cookie plus exact-origin and double-submit CSRF checks. Logout revokes the MySQL family and removes Redis active-session state. Bearer access also requires an active Redis/MySQL session and fails closed when the session store is unavailable.

## 6. RBAC

Seeded roles are `EMPLOYEE`, `SUPPORT_AGENT`, `KNOWLEDGE_MANAGER`, and `ADMINISTRATOR`. Implemented permissions are `user.read`, `user.manage`, and `audit.read`; future ticket/knowledge/AI permissions are data placeholders with no business endpoints. Controllers use `@PreAuthorize` permission checks. Administrators can create a user, read a user, replace roles, change status, and list roles. Optimistic versions protect status/role writes.

## 7. Audit

Implemented/reserved actions include `BOOTSTRAP_ADMIN`, `LOGIN_SUCCESS`, `LOGIN_FAILURE`, `REFRESH`, `REFRESH_REPLAY`, `LOGOUT`, `USER_CREATE`, `USER_DISABLE`, `ROLE_ASSIGN`, and `PERMISSION_CHANGE`. Metadata is explicitly allowlisted and never serializes a request body, password, token or Authorization header. User creation/status/role changes write audit data inside the application transaction. There is no audit update/delete API.

## 8. Error Handling

The unified response contains `code`, `message`, `details`, `requestId`, and `timestamp`. The global handler covers validation, malformed requests, authentication, authorization, not found, conflict and internal errors without returning stack traces.

## 9. Request ID

`RequestIdFilter` validates a client `X-Request-ID` or generates a UUID. It returns the header, stores it in MDC, places it in error responses and makes it available to audit events. Access logs include request ID, method, path, status, latency and authenticated user ID without credentials or tokens.

## 10. Docker

The root Compose topology defines `java-backend`, MySQL 8.4 and Redis 7.4. Java binds only to `127.0.0.1`, while MySQL/Redis remain on an internal data network. Qdrant remains behind the `later-phases` profile and has no Phase 1 integration. The Java Dockerfile uses a Maven/Temurin 21 build stage, a Temurin 21 runtime, a non-root user and an HTTP healthcheck.

`docker compose --env-file .env.example config --quiet` passed. The installed Docker CLI could not connect to `dockerDesktopLinuxEngine`; the user-local Docker Desktop executable exited without starting an engine. Compose startup, image build and health checks were therefore not run.

## 11. Tests

Unit/architecture tests cover BCrypt salting/matching, JWT claims/signature rejection, controller/repository separation and the absence of later-phase controllers. `Phase1AuthenticationIT` defines six ordered scenarios using MySQL 8.4 and Redis 7.4 Testcontainers: bootstrap/login/me/request ID, create/duplicate user, login/validation errors, employee allow/admin deny/refresh rotation, logout/revocation, and disable/audit evidence.

The local run executed 5 unit/architecture tests successfully. All 6 integration scenarios were explicitly skipped because Testcontainers found no valid Docker environment. They are implemented but are not claimed as locally passed.

## 12. CI

`.github/workflows/java.yml` uses Temurin Java 21, requires `docker info` to succeed, runs `./mvnw -B clean verify`, and then validates the root Compose configuration. The explicit Docker precheck prevents CI from silently accepting skipped Testcontainers scenarios. No remote CI run is available in this local-only task.

## 13. Validation Commands

```powershell
$env:JAVA_HOME="$env:TEMP\knowledgeops-jdk21\jdk-21.0.12.1+1"
Set-Location backend-java
.\mvnw.cmd -B spotless:apply test
.\mvnw.cmd -B clean verify

Set-Location ..
docker compose --env-file .env.example config --quiet
docker compose --profile later-phases --env-file .env.example -f docker-compose.yml -f infra/compose.application.yml config --quiet
.venv\Scripts\python.exe scripts\validate_architecture.py
git diff --check
```

The requested runtime commands could not be executed because the Docker daemon was unavailable:

```powershell
docker compose --env-file .env up -d --build mysql redis java-backend
Invoke-RestMethod http://localhost:8080/actuator/health
# authentication smoke flow
docker compose --env-file .env down
```

## 14. Test Results

| Check | Result | Evidence |
| --- | --- | --- |
| Java compile/package | PASS | Spring Boot executable jar built |
| Unit and ArchUnit | PASS | 5 run, 0 failures/errors/skips |
| Testcontainers integration | NOT RUN | 6 skipped; no Docker environment |
| Spotless | PASS | 60 Java files clean |
| Maven `clean verify` | PASS WITH SKIPS | Build success; integration tests skipped |
| Compose configuration | PASS | Phase 1 default config valid |
| Future overlay configuration | PASS | Valid with `--profile later-phases` |
| Phase 0 static architecture validation | PASS | `ARCHITECTURE_STATIC_VALIDATION=PASS` |
| Container/application smoke | NOT RUN | Docker engine unavailable |

## 15. Known Limitations

- Runtime startup, Flyway against MySQL 8.4, Redis-backed session checks, and the HTTP smoke flow still need one Docker-capable execution.
- Local/demo authentication uses one HMAC signing secret and has no key rotation service or external identity provider.
- Redis/MySQL checks favor immediate revocation over lower request latency; performance has not been measured.
- Department is the seeded `GENERAL` row only.
- Audit append-only behavior is enforced by application APIs, not by denying database-administrator writes.
- Runtime OpenAPI covers implemented endpoints; the frozen Phase 0 public contract still contains the recorded identity mismatch.

## 16. Architecture Deviations

`docs/CONTRACT_DEVIATION.md` records the deliberate corrections:

- Phase 1 uses unique `email` identity where the Phase 0 generated contract used `username`.
- Public registration is omitted to follow Product Scope; bootstrap/admin provisioning is used.
- Narrow status and role command endpoints replace a generic user update operation.
- The role remains `ADMINISTRATOR`, consistent with Product Scope and the frozen role schema, instead of introducing the brief's `ADMIN` shorthand.

## 17. What Was Intentionally NOT Implemented

Ticket business logic, Knowledge Base business logic, document upload/parsing/chunking, Python/FastAPI, RAG, Agent/tool calling, AI approval execution, Qdrant integration, DeepSeek/LLM/embedding, Vue/frontend, user-management UI, department administration, bulk import, Kafka, RabbitMQ, Kubernetes, GraphQL, gRPC, CQRS, event sourcing, Keycloak, OAuth Server, Spring Cloud and microservices were not implemented.

```text
PHASE_1_COMPLETE = false
SPRING_BOOT_RUNS = false
MYSQL_MIGRATION_PASS = false
AUTH_READY = false
JWT_READY = true
RBAC_READY = false
AUDIT_READY = false
TESTS_PASS = false
DOCKER_CONFIG_PASS = true
LATER_PHASE_CODE_IMPLEMENTED = false
READY_FOR_PHASE_2 = false
```

The false runtime flags mean “not proven by the required Docker-backed acceptance run,” not “implementation absent.” One successful Docker/CI `clean verify` plus the prescribed health/auth smoke flow is required before changing them to true.
