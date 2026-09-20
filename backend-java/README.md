# Java backend — Phase 1

Java 21 / Spring Boot 4.1 modular monolith foundation. Implemented modules are `auth`, `user`, `audit`, and `shared`. `ticket`, `knowledge`, and `ai` contain package documentation only.

From this directory run `./mvnw clean verify` (`mvnw.cmd clean verify` on Windows). Tests named `*IT` use MySQL 8.4 and Redis 7.4 Testcontainers and are skipped only when Docker is unavailable; unit and architecture tests still run.

The runtime requires `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `REDIS_PASSWORD`, and a JWT secret of at least 32 UTF-8 bytes. See the repository README and `.env.example` for Docker usage. Swagger UI is at `/swagger-ui.html`; health is at `/actuator/health` and `/api/v1/health`.
