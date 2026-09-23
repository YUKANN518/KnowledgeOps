# Portfolio summary

## Project

KnowledgeOps

## Positioning

Java Backend / Software Engineering portfolio project for internship applications and technical interviews.

## Problem

Small internal teams often need one place to track support requests and publish reusable operational knowledge. KnowledgeOps connects a simple ticket workflow with governed articles and private documents, while keeping identity, permissions, audit, and data consistency visible in the implementation.

## Key engineering topics

- Authentication and refresh-token rotation
- RBAC and resource-level authorization
- REST API design and consistent errors
- Transaction boundaries and audit consistency
- Optimistic concurrency control
- Flyway database migrations and relational constraints
- Secure local file handling
- Testcontainers integration testing
- Docker Compose runtime verification

## What I built

I built a Vue frontend and Spring Boot modular monolith backed by MySQL and Redis. The system supports login/session restoration/logout, administrator-created users and roles, scoped ticket collaboration, article publishing, secure document upload/download/archive, audit events, request IDs, and a containerized local runtime.

The frontend implements permission-aware routes and controls, Axios single-flight refresh, loading/empty/error states, and responsive screens. Backend authorization remains authoritative for every protected resource.

## What I learned

- Authentication requires coordinated database, cache, cookie, CSRF, and client behavior; a signed JWT alone is insufficient for immediate revocation.
- RBAC answers what a user may generally do, while resource checks answer whether the user may act on this specific ticket or document.
- Transactions must include audit writes when audit evidence is part of the business guarantee.
- Optimistic locking turns silent lost updates into explicit conflicts that a client can handle.
- File security is a chain of checks: filename, type, content, generated storage name, containment, and authorized retrieval.
- Integration tests are more credible when they exercise the same MySQL and Redis products used at runtime.
- Scope control improves a portfolio project when each delivered capability can be demonstrated and explained.

## Scope decisions

The system remains a modular monolith. Microservices and Kubernetes would add deployment and consistency costs without helping this project's learning goals. Local persistent storage is adequate for a single-machine demo, so MinIO or another object store was not added. Elasticsearch/full-text search and AI/RAG were excluded because the final product flow does not require them.

For a real production system, object storage, stronger observability, automated delivery, backups, key rotation, and a deployment platform would be evaluated from measured needs. They are not claimed as current features.
