# Phase 5 — Portfolio Finish Report

Verification date: 2026-09-23 (Asia/Shanghai)

## Portfolio deliverables

- `README.md` — final GitHub project page.
- `docs/ARCHITECTURE.md` — concise implemented architecture reference.
- `docs/DEMO_SCRIPT.md` — repeatable 3–5 minute interview demonstration.
- `docs/INTERVIEW_GUIDE.md` — 20 implementation-based interview questions and answers.
- `docs/RESUME_BULLETS.md` — three Chinese and three English resume bullets.
- `docs/PORTFOLIO_SUMMARY.md` — positioning, engineering topics, learning, and scope decisions.
- `docs/README.md` — separates final implementation reference from Phase 0 design history.
- `docs/images/` — six screenshots from the running application.
- `PHASE_5_PORTFOLIO_FINISH_REPORT.md` — final evidence and release-readiness record.

No backend or frontend product feature was added. The runtime smoke output gained only `supportId` so the existing assignment UI can be demonstrated without querying the database manually. `.gitignore` gained local upload/database/editor exclusions, and the root Compose comment was updated to describe the final topology.

The existing architecture validator recursively inspected installed `node_modules` and still expected the pre-frontend Compose service set. It was minimally corrected to ignore generated/dependency directories and recognize the final frontend service plus its loopback/network boundary; all architecture contracts then passed.

## README

The root README now gives a recruiter a short path through:

- product overview and honest positioning;
- four primary screenshots plus two additional view links;
- authentication/security, ticket, knowledge, document, and engineering features;
- exact backend, frontend, data, runtime, and test stack;
- simple system, authentication, authorization, and domain diagrams;
- verified Docker Compose Quick Start and health URLs;
- bootstrap/demo account guidance without publishing a real secret;
- frontend/backend verification commands;
- interview material and explicit limitations.

The previous README said Phase 3 was current and claimed the frontend was absent. That stale content was replaced. The existing architecture document also described future Python/Qdrant/approval behavior as if it were the system architecture; it was rewritten around the implemented Vue, Spring Boot, MySQL, Redis, and private-volume runtime.

Phase 0 proposal files remain useful design history. Each high-risk entry point now carries a prominent archive notice, and `docs/README.md` tells readers which documents are authoritative for the runnable product.

## Screenshots

All screenshots were captured from the live Vue/Nginx service at 1366×768 using disposable `.local` accounts and smoke-test data:

- `docs/images/login.png`
- `docs/images/dashboard.png`
- `docs/images/tickets.png`
- `docs/images/ticket-detail.png`
- `docs/images/knowledge-articles.png`
- `docs/images/documents.png`

The screenshots show Employee, Support Agent, and Knowledge Manager views. They contain no password, token, secret, Authorization header, local filesystem path, real personal account, or API key. Browser automation connector initialization failed twice, so a temporary ignored-environment Python helper drove preinstalled headless Chrome through its local DevTools protocol. The helper was deleted after visual inspection; only PNG artifacts remain.

## Interview materials

- **Demo Script:** follows login/session restore, employee ticket creation/comment, support assignment/transition, IDOR denial, article publishing, document upload/download/archive, and engineering evidence in under five minutes.
- **Interview Guide:** answers architecture, JWT/refresh separation, Redis purpose, RBAC/resource authorization, IDOR, Flyway, transactions, optimistic locking, secure files, Testcontainers, audit, rotation, frontend permissions, real bugs, and production-scope questions from actual code.
- **Resume Bullets:** provides restrained Chinese and English descriptions aimed at Java Backend / Software Engineering internships.
- **Portfolio Summary:** explains what was built, what was learned, and why microservices, Kubernetes, object storage, search, and AI were excluded.

## Verification

### Frontend

- `npm run lint`: PASS
- `npm run type-check`: PASS
- `npm test`: PASS — 2 files, 6 tests
- `npm run build`: PASS
- `npm audit --omit=dev`: PASS — 0 vulnerabilities
- Known non-blocking build warning: the complete Element Plus dependency chunk is larger than 500 kB.

### Backend

The default host JDK is 17, so test execution used the installed JDK 25 compiler with Java release 21. To avoid the previously documented post-test google-java-format/JDK incompatibility, regression ran the test goals directly:

```text
.\mvnw.cmd clean test failsafe:integration-test failsafe:verify
```

Result: `BUILD SUCCESS`.

- Unit and architecture tests: 6 passed, 0 failed, 0 errors, 0 skipped.
- Testcontainers integration tests: 17 passed, 0 failed, 0 errors, 0 skipped.
- Phase 1 authentication scenarios: 6 passed.
- Phase 2 ticket scenarios: 7 passed.
- Phase 3 knowledge scenarios: 4 passed.
- Real MySQL 8.4 and Redis 7.4 containers started for each integration suite.
- Flyway validated and applied V1, V2, and V3 to each integration database.

This report does not claim that the normal host `clean verify` lifecycle passed: its final Spotless step remains affected by the documented formatter/JDK and Windows line-ending environment. No business code or formatter configuration was changed to hide that limitation.

### Docker and runtime smoke

- Docker Engine 29.8.0 and Docker Compose 5.5.1: available.
- `docker compose --env-file .env config --quiet`: PASS.
- Frontend and Java images rebuilt successfully.
- `frontend`, `java-backend`, `mysql`, and `redis`: all healthy.
- Frontend `/health`: `healthy`.
- Backend `/actuator/health`: `UP`.
- Runtime Flyway history: V1/V2/V3 successful.
- Real frontend-proxy smoke: authentication, session restore, ticket, knowledge, documents, resource 403, refresh rotation, and logout rejection all PASS.
- The smoke flow created disposable Employee, Support Agent, and Knowledge Manager users, assigned and transitioned a ticket, published an article, uploaded/downloaded byte-matched TXT/PDF files, archived a document, and rejected employee category creation.
- The environment was stopped with `docker compose --env-file .env down` after evidence collection.

Docker Desktop initially failed before testing because stale Windows AF_UNIX endpoints prevented `sailor-ingest.sock` and the secrets engine socket from being renamed. All Docker Desktop processes were stopped, the runtime-only socket directories were moved to timestamped quarantine paths, and Docker was restarted without resetting images or volumes. Engine, Testcontainers, Compose, and smoke verification then passed.

## Security and repository audit

- Local `.env` is ignored and not tracked; `.env.example` contains placeholders and an empty provider key.
- Tracked files and all Git revisions were checked for common private-key, cloud-key, GitHub-token, and provider-token patterns; no credential was found.
- No tracked upload, database, `node_modules`, build output, IDE cache, or local virtual environment artifact exists.
- `.gitignore` covers environment files, keys, secrets, uploads, database files, build outputs, dependencies, Python caches, IDE data, and logs.
- README and final documentation local links resolve to tracked deliverables.
- Existing `scripts/validate_architecture.py`: PASS after excluding generated/dependency directories from Markdown discovery.
- Screenshot visual inspection found only disposable `.local` demo identities and business identifiers.

## Scope control

- No new business module, API, table, permission, entity, migration, ticket behavior, knowledge behavior, or dashboard behavior.
- No backend or frontend architecture refactor.
- No AI, RAG, embedding, vector search, Python application, Elasticsearch, MinIO, WebSocket, notification, workflow, analytics, Kubernetes, cloud deployment, or microservice implementation.
- Product feature scope is frozen and development stops at Phase 5.

## Known limitations

- Documents use a local persistent Docker volume rather than object storage.
- There is no full-text search or document-content parsing.
- The application has no production deployment, backup automation, external identity provider, or signing-key rotation service.
- CI runs project checks but is not a production delivery pipeline.
- The frontend build retains a dependency chunk-size warning.
- Phase 0 planning artifacts describe ideas that were deliberately not delivered; archive notices prevent them from being mistaken for runtime features.

## Final status

```text
PHASE_5_COMPLETE = true
PORTFOLIO_README_READY = true
SCREENSHOTS_READY = true
ARCHITECTURE_DOC_READY = true
DEMO_SCRIPT_READY = true
INTERVIEW_GUIDE_READY = true
RESUME_BULLETS_READY = true
PORTFOLIO_SUMMARY_READY = true
QUICK_START_VERIFIED = true
SECRET_AUDIT_PASS = true
FRONTEND_VERIFICATION_PASS = true
BACKEND_REGRESSION_PASS = true
DOCKER_RUNTIME_SMOKE_PASS = true
GIT_WORKTREE_CLEAN = true
BACKEND_SCOPE_EXPANDED = false
FRONTEND_SCOPE_EXPANDED = false
AI_CODE_IMPLEMENTED = false
PROJECT_FEATURE_SCOPE_FROZEN = true
READY_FOR_PUBLIC_GITHUB = true
READY_FOR_INTERVIEW_DEMO = true
READY_FOR_JOB_APPLICATION = true
STOP_DEVELOPMENT = true
```
