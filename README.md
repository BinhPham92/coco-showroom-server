# coco-showroom-server

Spring Boot 3.3 · Java 21 · PostgreSQL · Flyway · Docker Compose

Backend API for [coco-showroom-client](https://github.com/yourusername/coco-showroom-client).  
Follows the migration plan in the frontend's `docs/PRD/13-backend-integration.md`.

## Migration status

| Week | Feature         | Status      |
|------|-----------------|-------------|
| W1   | Contact form    | ✅ Done     |
| W2   | Products read   | ⏳ Next     |
| W3   | Auth (JWT)      | 🔜 Planned  |
| W4   | Cart reconcile  | 🔜 Planned  |
| W5   | Orders (create) | 🔜 Planned  |
| W6   | Orders (read)   | 🔜 Planned  |

## Quick start (local dev)

**Prerequisites:** Docker Desktop, Java 21, Maven 3.9+

```bash
# 1. Copy env file
cp .env.example .env

# 2. Start Postgres
docker compose -f docker-compose.yml up db -d

# 3. Run the app (Flyway migrations run automatically)
mvn spring-boot:run

# API is at http://localhost:8080
```

## Running tests

```bash
mvn test
```

Tests use H2 in-memory with `MODE=PostgreSQL`. Flyway is disabled; Hibernate creates the schema.

## Environment variables

| Variable                 | Default                          | Required in prod |
|--------------------------|----------------------------------|------------------|
| `SPRING_DATASOURCE_URL`  | `jdbc:postgresql://localhost/...`| ✅               |
| `SPRING_DATASOURCE_USERNAME` | `cocoshowroom`              | ✅               |
| `SPRING_DATASOURCE_PASSWORD` | `cocoshowroom`              | ✅               |
| `APP_CORS_ORIGINS`       | `http://localhost:3031`          | ✅               |
| `JWT_SECRET`             | dev placeholder                  | ✅ (≥ 32 chars)  |

## Deploying to VPS

```bash
# On the VPS — first time only
git clone https://github.com/yourusername/coco-showroom-server
cd coco-showroom-server
cp .env.example .env
# Edit .env with production values

# Issue TLS cert (replace with your domain)
docker compose run --rm certbot certonly --webroot \
  --webroot-path /var/www/certbot \
  -d api.cocoshowroom.vn

# Start everything
docker compose up -d --build
```

**Subsequent deploys:**
```bash
git pull && docker compose up -d --build app
```

## Project structure

```
src/main/java/com/cocoshowroom/server/
├── CocoshowroomApplication.java
├── config/
│   ├── AppProperties.java    # Typed @ConfigurationProperties
│   └── SecurityConfig.java   # CORS + stateless security
├── contact/                  # W1: contact form submissions
│   ├── ContactController.java
│   ├── ContactService.java
│   ├── ContactRepository.java
│   ├── ContactSubmission.java (entity)
│   └── ContactRequest.java   (record DTO)
└── shared/
    ├── ApiErrorResponse.java        # { code, message, traceId }
    └── GlobalExceptionHandler.java  # Maps exceptions → HTTP responses
```

## API contract

All endpoints are under `/v1/`. Error responses match the shape the frontend's `fetcher.ts` expects:

```json
{ "code": "validation_error", "message": "email: must be a valid email", "traceId": "..." }
```

### POST /v1/contact

```json
// Request
{ "name": "Nguyen Van A", "email": "vana@example.com", "subject": "...", "message": "..." }

// 201 Created
{ "ok": true }

// 422 Unprocessable Entity
{ "code": "validation_error", "message": "email: must be a valid email address", "traceId": "..." }
```
