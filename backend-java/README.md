# Java Backend

Java 21 / Spring Boot modular monolith for authentication, user and role management, tickets, knowledge content, audit logging, and shared API infrastructure.

Run the complete unit and Testcontainers integration suite from this directory:

```bash
./mvnw clean verify
```

On Windows, run `mvnw.cmd clean verify`. Integration tests require a working Docker Engine and use MySQL 8.4 and Redis 7.4. For runtime configuration and the full-stack Compose setup, see the [repository README](../README.md) and [`.env.example`](../.env.example).

The health endpoints are `/actuator/health` and `/api/v1/health`; the OpenAPI UI is available at `/swagger-ui.html`.
