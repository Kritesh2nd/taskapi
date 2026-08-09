# Task API

A Spring Boot 3 REST API for managing users and their tasks, backed by PostgreSQL.

## Stack

- Java 17
- Spring Boot 3.3 (Web, Data JPA, Validation, Actuator)
- PostgreSQL
- Lombok
- springdoc-openapi (Swagger UI)
- JUnit 5 + MockMvc (tests run against in-memory H2, no Postgres needed)

## Project layout

```
src/main/java/com/techaxis/taskapi/
├── controller/    REST endpoints
├── service/       business logic, transactions
├── repository/    Spring Data JPA repositories
├── entity/        JPA entities
├── dto/           request/response records (never expose entities directly)
├── exception/     custom exceptions + global @RestControllerAdvice handler
└── config/        (reserved for cross-cutting config, e.g. security later)
```

## Running locally

### 1. Start PostgreSQL

```bash
docker compose up -d
```

This starts Postgres on `localhost:5432` with database `taskapi`, user `taskapi_user`, password `changeme` (override via `DB_USERNAME` / `DB_PASSWORD` env vars — see `application.properties`).

### 2. Run the app

```bash
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080`. Hibernate will auto-create/update the schema (`ddl-auto=update`) — fine for dev, swap for Flyway/Liquibase migrations before production.

### 3. Explore the API

Swagger UI: `http://localhost:8080/swagger-ui.html`
Health check: `http://localhost:8080/actuator/health`

## API overview

### Users — `/api/v1/users`

| Method | Path              | Description       |
|--------|-------------------|--------------------|
| POST   | `/`               | Create a user      |
| GET    | `/`               | List all users     |
| GET    | `/{id}`           | Get a user by id   |
| PUT    | `/{id}`           | Update a user      |
| DELETE | `/{id}`           | Delete a user      |

### Tasks — `/api/v1/tasks`

| Method | Path      | Description                                              |
|--------|-----------|------------------------------------------------------------|
| POST   | `/`       | Create a task (`ownerId` required)                        |
| GET    | `/`       | List tasks, paginated. Optional filters: `ownerId`, `status` |
| GET    | `/{id}`   | Get a task by id                                           |
| PUT    | `/{id}`   | Update a task                                              |
| DELETE | `/{id}`   | Delete a task                                              |

`status` is one of `TODO`, `IN_PROGRESS`, `DONE`.

Pagination params on `GET /api/v1/tasks`: `page`, `size`, `sort` (e.g. `?page=0&size=10&sort=dueDate,asc`).

### Example requests

```bash
# Create a user
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Ada Lovelace", "email": "ada@example.com"}'

# Create a task for that user (assume id 1)
curl -X POST http://localhost:8080/api/v1/tasks \
  -H "Content-Type: application/json" \
  -d '{"title": "Write the algorithm", "description": "For the Analytical Engine", "ownerId": 1}'

# List a user's TODO tasks
curl "http://localhost:8080/api/v1/tasks?ownerId=1&status=TODO"
```

### Error format

Validation errors and business errors return a consistent shape:

```json
{
  "timestamp": "2026-08-09T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/users",
  "fieldErrors": { "email": "email must be a valid email address" }
}
```

## Running tests

```bash
./mvnw test
```

Tests use an in-memory H2 database (`application-test.properties`) so they don't require Postgres to be running.

## Building a jar

```bash
./mvnw clean package
java -jar target/taskapi-1.0.0.jar
```

## Building the Docker image

```bash
docker build -t taskapi:1.0.0 .
docker run -p 8080:8080 \
  -e DB_USERNAME=taskapi_user -e DB_PASSWORD=changeme \
  --network host taskapi:1.0.0
```

## Notes / what's intentionally left out

This is a clean starting point, not a production-hardened service. Before shipping it for real, add:

- **Auth** — currently every endpoint is open. Add Spring Security + JWT if this needs to be user-facing.
- **DB migrations** — replace `ddl-auto=update` with Flyway or Liquibase.
- **Rate limiting / CORS config** — add if this will be called from a browser frontend.
- **Structured logging / tracing** — Actuator + Micrometer if this goes into a real cluster.
