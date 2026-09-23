# KnowledgeOps 3–5 minute demo

## Preparation

1. Start the stack with `docker compose --env-file .env up -d --build`.
2. Run `.\frontend\tests\runtime-smoke.ps1` from the repository root. Keep the generated Employee, Support Agent, and Knowledge Manager emails from its output.
3. Open <http://localhost:5173>. Use the local demo passwords defined in the smoke script.
4. Keep `docker compose ps`, the Flyway migrations, and the integration test directory ready in separate editor tabs.

All accounts and content created by the smoke script are disposable local demo data.

## 0:00–0:35 — Login and dashboard

Log in as the generated Employee.

- Point out that the UI shows the current role and permissions returned by the backend.
- Reload once to demonstrate session restoration through the HttpOnly refresh cookie.
- Explain that the access token remains in memory and concurrent 401 responses share one refresh request.

## 0:35–1:35 — Ticket flow

- Open **Tickets** and create a ticket with a short operational problem.
- Open its detail page and add a comment.
- Log out, then log in as the Support Agent.
- Open the same ticket, assign it to the Support Agent UUID prepared from the API result, and move it to **In Progress**.

Explain that assignment, state transitions, optimistic version checks, and audit writes happen in the backend. The implemented state machine rejects unsupported transitions with HTTP 409.

## 1:35–2:05 — Resource authorization

Use the Employee account to open a ticket created by a different employee, or explain the existing smoke result if the other account is not open in the browser.

- The backend returns 403 even when the caller knows the ticket UUID.
- The frontend permission checks improve navigation, while Spring Security and resource checks remain the security boundary.

This is the project's clearest IDOR example.

## 2:05–3:10 — Knowledge and documents

Log in as the Knowledge Manager.

- Create a draft article, edit it, and publish it.
- Upload a small TXT or PDF document.
- Show the document list and archive action.

Then switch to the Employee:

- Read the published article.
- Download the active document.
- Note that draft/archived content and management controls are unavailable.

Mention the 20 MiB limit, extension/MIME/signature checks, UUID storage name, private volume, and backend download authorization.

## 3:10–4:15 — Engineering evidence

Show these repository areas briefly:

- `backend-java/src/main/resources/db/migration/` — Flyway `V1`–`V3`.
- `backend-java/src/test/java/com/knowledgeops/` — Testcontainers integration suites.
- `docker-compose.yml` — frontend, backend, MySQL, Redis, and uploads volume.
- `frontend/tests/runtime-smoke.ps1` — repeatable full-stack smoke flow.
- `PHASE_5_PORTFOLIO_FINISH_REPORT.md` — final verification evidence and limitations.

Explain that MySQL is the business source of truth, Redis validates active sessions, and audit metadata intentionally excludes secrets and content.

## 4:15–5:00 — Close

Describe the project as a deliberately scoped internal tool that demonstrates Java backend fundamentals: authentication, authorization, transactions, concurrency, migrations, secure file I/O, integration testing, and containerized runtime.

End the environment after the interview with `docker compose --env-file .env down`.
