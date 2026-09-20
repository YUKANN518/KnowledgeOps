# Phase 1 — Java backend foundation

## Implemented

Java 21 / Spring Boot 4.1.1 Maven application, modular packages, Flyway-managed MySQL schema, JPA identity entities, Spring Security, short-lived JWT access tokens, rotating opaque refresh tokens, Redis-backed live session checks, fixed-role permission RBAC, request correlation, unified errors, append-only audit writes, bootstrap administrator, health/OpenAPI endpoints, multi-stage container image, unit/architecture/integration tests and Java CI.

No public registration exists. `BOOTSTRAP_ADMIN_ENABLED=true` creates the first administrator only when the configured email is absent and never resets an existing password. Administrators can create the minimum synthetic/demo users, replace their roles, and change status. There is no user-management UI, search, bulk import, or department administration.

## Architecture mapping

- `auth`: login, refresh rotation/replay handling, logout, JWT, Redis session state, CSRF checks for cookie-authenticated refresh/logout.
- `user`: User/Department/fixed Role/Permission entities and narrow admin application services.
- `audit`: allowlisted metadata and application-level append-only repository; critical user/role/status changes share the business transaction.
- `shared`: error envelope, request ID/MDC/access log, health and OpenAPI metadata.
- `ticket`, `knowledge`, `ai`: package documentation only. No entity, repository, controller, or migration exists.

Controllers use DTOs and method-level permission checks. They do not expose entities. Application services own transactions; repository adapters own persistence details. ArchUnit prevents API packages from accessing repositories directly.

## Authentication and token lifecycle

BCrypt cost 12; password input 12–72 chars and never returned/logged. Access JWT expires in 10 minutes by default and includes `sub`, `roles`, `iat`, `exp`, `jti`, and `sid`. Authorities are reloaded from MySQL, so role/status changes do not trust stale role claims.

Refresh tokens are random 256-bit opaque values, only SHA-256 hashes are stored, and every refresh consumes the previous record and creates a new one in the same family. Reuse revokes the family. The HttpOnly refresh cookie and readable CSRF cookie are scoped to `/api/v1/auth`; refresh/logout require exact configured Origin plus matching `X-CSRF-Token`. Logout revokes the MySQL family and deletes Redis session state. Every bearer request requires both Redis ACTIVE and a current unrevoked MySQL family; Redis failure fails closed with 503.

## Database

`V1__identity_auth_audit.sql` creates eight Phase 1 tables: departments, users, roles, permissions, user_roles, role_permissions, refresh_tokens, audit_logs. It seeds `GENERAL`, four role codes, Phase 1 permissions, and named future permission placeholders only. Hibernate runs `ddl-auto=validate`; Flyway owns schema creation. The 17 later-phase target tables are not migrated.

## Audit

Implemented events: BOOTSTRAP_ADMIN, LOGIN_SUCCESS, LOGIN_FAILURE, REFRESH, REFRESH_REPLAY, LOGOUT, USER_CREATE, USER_DISABLE, ROLE_ASSIGN, and the reserved PERMISSION_CHANGE. Metadata is explicitly constructed from email hash/result/status/role names; request bodies, passwords, tokens, and Authorization headers are never serialized. Audit rows have no update API.

## Testing

Unit tests verify salted BCrypt and JWT required claims/tamper rejection. ArchUnit verifies controller/repository boundaries and prevents later-phase controllers. `Phase1AuthenticationIT` uses real MySQL 8.4 and Redis 7.4 Testcontainers for Flyway/JPA startup, bootstrap, success/failure login, create/duplicate user, request ID/error envelope, employee allow/admin deny, refresh rotation, logout revocation, disabled-user rejection and audit evidence. Testcontainers tests skip when Docker is genuinely unavailable; CI runs them on GitHub-hosted Docker.

## Docker

The root Compose default topology is Java + MySQL + Redis. Qdrant is retained behind the `later-phases` profile and has no Phase 1 integration. Java uses a multi-stage Java 21 image, a non-root runtime user, explicit healthcheck, and publishes only `127.0.0.1:8080`. MySQL and Redis remain on an internal data network.

## Known limitations

- This is a local/demo security foundation: one HMAC secret, one application instance, no key rotation service or external identity provider.
- Redis/MySQL session checks intentionally trade latency for immediate revocation. Performance tuning waits for measurements.
- Department is the seeded GENERAL row only. Department management belongs to a later admin slice.
- Audit append-only behavior is enforced through application APIs; a database administrator can still alter rows.
- Runtime OpenAPI documents implemented endpoints. The Phase 0 username/email mismatch is recorded in [CONTRACT_DEVIATION.md](CONTRACT_DEVIATION.md).
