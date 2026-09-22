# Phase 2 — Simple Ticket System Report

## Implemented

- Ticket creation, paginated listing, detail lookup, priority/status filtering.
- Append-only ticket comments with list support.
- Support/Admin assignment and reassignment with active support-user validation.
- Centralized ticket status policy: `OPEN -> IN_PROGRESS`, `IN_PROGRESS -> OPEN|RESOLVED`, `RESOLVED -> CLOSED`.
- Resource-level RBAC for Employee, Support Agent, and Administrator, including employee IDOR protection.
- JPA `@Version` optimistic concurrency with `expectedVersion` checks and HTTP 409 conflicts.
- Ticket create/assign/comment/transition changes and their audit records share one transaction.

## Database

Flyway migration `V2__ticket_system.sql` creates only the Phase 2 business tables:

- `tickets`
- `ticket_comments`

It also seeds the ticket permissions required by Phase 2. The schema has primary keys, user/department/ticket foreign keys, priority/status checks, and focused indexes for creator, department, assignee, status, creation time, and comment ordering. Hibernate remains configured with `ddl-auto: validate`; Flyway owns DDL.

Runtime MySQL 8.4 verification showed Flyway V1 and V2 at `success=1`, both Phase 2 tables present, all declared FK/CHECK constraints present, and all declared indexes present.

## API

- `POST /api/v1/tickets`
- `GET /api/v1/tickets`
- `GET /api/v1/tickets/{id}`
- `POST /api/v1/tickets/{id}/comments`
- `GET /api/v1/tickets/{id}/comments`
- `POST /api/v1/tickets/{id}/assignments`
- `POST /api/v1/tickets/{id}/transitions`

Assignment and transition paths follow the frozen API contract. List pagination defaults to page 0 and size 20, rejects size above 100, and supports `status` and `priority` filters.

## Permissions

- Employee: create, read own tickets, add/read comments on own tickets.
- Support Agent: read department tickets, assign department tickets to valid support users, add/read comments, transition tickets assigned to self.
- Administrator: read, assign, and transition all tickets; valid assignees must still be active support-capable users in the ticket department.

An authenticated employee reading another employee's ticket receives HTTP 403 with `TICKET_ACCESS_DENIED`. Method-level permission failures also return HTTP 403.

## Status Flow

The domain policy allows only:

- `OPEN -> IN_PROGRESS`
- `IN_PROGRESS -> OPEN`
- `IN_PROGRESS -> RESOLVED`
- `RESOLVED -> CLOSED`

Starting work requires an assignee. Other transitions return HTTP 409 with `INVALID_TICKET_STATUS`.

## Audit

The implementation records:

- `TICKET_CREATED`
- `TICKET_ASSIGNED`
- `TICKET_STATUS_CHANGED`
- `TICKET_COMMENT_ADDED`

Runtime verification found six events for the smoke ticket: one create, one assignment, one comment, and three status changes. Every event had the correct actor and a request ID. Metadata contained only identifiers, priority/status, or from/to state; it contained no description, comment content, password, JWT, refresh token, or Authorization header.

## Tests

Command: `./mvnw clean verify`

- Unit/architecture tests: 6 passed, 0 failed, 0 skipped.
- Testcontainers integration tests: 13 passed, 0 failed, 0 skipped.
- Phase 2 integration scenarios: 7 passed, including create, own read, IDOR denial, Support/Admin read, assignment, invalid assignee, comments, complete status flow, invalid transition, pagination, stale-version conflict, unauthenticated 401, permission 403, audit, and migration constraints.
- Phase 1 authentication regression: 6 integration scenarios passed, including login, JWT, refresh rotation, logout, RBAC, audit, request ID, and Flyway startup.

## Runtime Verification

- Docker Engine 29.8.0 and Docker Compose 5.5.1 responded successfully.
- `docker compose config` passed after creating the ignored local `.env` from `.env.example`.
- `mysql`, `redis`, and `java-backend` all reached `healthy`.
- `/actuator/health` returned `UP`; `/api/v1/health` returned `UP`.
- Real HTTP smoke flow passed: Admin login, Employee login/create/own-read, second Employee IDOR denial (403), Support login/self-assignment, `OPEN -> IN_PROGRESS`, comment, `IN_PROGRESS -> RESOLVED`, `RESOLVED -> CLOSED`, final detail read, and audit verification.
- Final smoke ticket status was `CLOSED`, version 5, with one retrievable comment.
- MySQL and Redis connectivity were exercised by the authenticated flow; MySQL schema and audit rows were also queried directly in the running container.
- Docker Desktop had stale local AF_UNIX socket entries after abrupt engine stops. The stale runtime directories were quarantined and the engine restarted; subsequent tests and runtime verification passed.

## Known Limitations

- Phase 2 uses the existing single seeded department; department administration remains outside this phase.
- Ticket list order is fixed to newest first with ID as a stable tie-breaker.
- Comments are public to authorized ticket participants; internal comment visibility is outside this simplified phase.

## What Was Not Implemented

- Assignment history or status history tables/APIs.
- Workflow engine, event bus, Kafka, WebSocket, search, cache, attachments, comment editing/deletion, mention, or reactions.
- Knowledge Base, document pipeline, Python, AI, RAG, LLM, Agent, MCP, Qdrant, or Vue code.

## Completion

```text
PHASE_2_COMPLETE = true
TICKET_CREATE_READY = true
TICKET_QUERY_READY = true
TICKET_COMMENT_READY = true
TICKET_ASSIGN_READY = true
TICKET_STATUS_READY = true
TICKET_AUTH_READY = true
AUDIT_READY = true
TESTS_PASS = true
RUNTIME_SMOKE_PASS = true
PHASE_1_REGRESSION_PASS = true
AI_CODE_IMPLEMENTED = false
READY_FOR_PHASE_3 = true
```
