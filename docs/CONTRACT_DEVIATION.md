# Contract deviation — Phase 1 identity API

Status: Accepted for implementation, 2026-09-20. This file makes an implementation correction explicit; it does not silently rewrite the frozen Phase 0 generated contract.

Phase 0 `java-public.openapi.json` modelled a user with `username` and allowed a generic user update. Phase 1 explicitly requires a unique email identity and its security scope needs narrower commands. The implemented API therefore uses `email` in login/user DTOs, assigns new users to the seeded `GENERAL` department, and exposes separate status and role commands:

- `POST /api/v1/users` — admin creates an account; no public register endpoint.
- `PATCH /api/v1/users/{id}/status` — optimistic-version status change.
- `PUT /api/v1/users/{id}/roles` — optimistic-version role replacement.
- `GET /api/v1/users/me`, `/api/v1/users/{id}`, and `/api/v1/roles` retain the intended resource paths.

The role code remains `ADMINISTRATOR`, matching Product Scope and the frozen role schema; the Phase 1 brief's shorthand `ADMIN` is not introduced as a second role.

The full Phase 0 `/users` list, department administration, and future permissions have no controller implementation in Phase 1. Future contract regeneration should replace `username` with `email`, include the command endpoints above, and keep backward compatibility decisions explicit.
