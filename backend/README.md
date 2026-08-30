# Backend

Quarkus/Java backend for Developer Analytics.

## Requirements

- Java 21
- Maven 3.9+
- PostgreSQL 16+ for deployed environments
- Docker when using Quarkus Dev Services for local/test PostgreSQL

## Commands

```bash
mvn test
mvn verify
mvn quarkus:dev
```

## Database

PostgreSQL is the application's persistence store. Flyway owns the schema and migrations live in:

```text
src/main/resources/db/migration/
```

The first baseline migration creates `application_metadata`. Hibernate schema generation is disabled; application mappings must match the Flyway-managed schema.

When no JDBC URL is configured in development/test, Quarkus Dev Services may start a PostgreSQL container automatically when Docker is available.

For an externally managed database, configure for example:

```text
QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://localhost:5432/developer_analytics
DB_USERNAME=developer_analytics
DB_PASSWORD=change-me
```

## Smoke endpoint

```text
GET /api/health/application
```

## Production container

Build the backend image from the repository root:

```bash
docker build -f backend/Dockerfile -t developer-analytics-backend .
```

The Dockerfile performs a Maven/Quarkus build in a builder stage and copies the Quarkus fast-jar runtime into a Java 21 JRE image. The runtime process uses a non-root user and listens on port `8080`.

The same backend image is intentionally suitable for reuse by the future background-worker service; worker-specific startup behaviour will be added when the worker runtime mode is implemented.
