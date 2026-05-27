# coco-showroom-server

Spring Boot 3.3 · Java 21 · PostgreSQL · Flyway · Docker Compose

Backend API for [coco-showroom-client](https://github.com/yourusername/coco-showroom-client).  
Follows the migration plan in the frontend's `docs/PRD/13-backend-integration.md`.

## Migration status

| Week | Feature                       | Status      |
|------|-------------------------------|-------------|
| W1   | Contact form                  | ✅ Done     |
| W2   | Products read                 | ✅ Done     |
| W3   | Auth (JWT)                    | ✅ Done     |
| W4   | Cart reconcile                | ✅ Done     |
| W5   | Orders (create)               | ✅ Done     |
| W6   | Orders (read)                 | ✅ Done     |
| W7   | Reviews (collect + display)   | ✅ Done     |
| W8   | OAuth/SSO (Google + Facebook) | ✅ Done     |
| W9   | Production hardening          | ✅ Done     |
| W10  | Transactional emails          | ✅ Done     |
| W11  | Staff / Admin API             | ✅ Done     |

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
| `ADMIN_EMAIL`            | `admin@cocoshowroom.vn`          | ✅               |
| `ADMIN_PROVIDER`         | `google`                         | ✅               |
| `GOOGLE_CLIENT_ID`       | —                                | ✅               |
| `MAIL_HOST`              | `smtp.sendgrid.net`              | ✅               |
| `MAIL_PORT`              | `587`                            | ✅               |
| `MAIL_USERNAME`          | `apikey`                         | ✅               |
| `MAIL_PASSWORD`          | —                                | ✅               |
| `MAIL_FROM_ADDRESS`      | `no-reply@cocoshowroom.vn`       | ✅               |
| `MAIL_FROM_NAME`         | `Coco Showroom`                  | ✅               |

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
│   └── SecurityConfig.java   # CORS + stateless JWT resource server
├── auth/                     # W3/W8: JWT auth + OAuth/SSO
│   ├── AuthController.java   # POST /social, POST /sign-out, GET+PATCH /me
│   ├── AuthService.java
│   ├── JwtService.java
│   ├── AdminUserInitializer.java
│   ├── User.java / UserRepository.java
│   ├── UserIdentity.java / UserIdentityRepository.java   # W8
│   ├── OAuthProvider.java    # enum: GOOGLE, FACEBOOK
│   └── social/
│       ├── SocialTokenVerifier.java  # interface
│       ├── SocialVerifierFactory.java
│       ├── GoogleVerifier.java       # JWKS-based id_token verification
│       └── FacebookVerifier.java     # Graph API access_token verification
├── cart/                     # W4: cart reconciliation
├── contact/                  # W1: contact form submissions
├── newsletter/               # newsletter subscribe/unsubscribe
├── order/                    # W5/W6: order creation + history
├── product/                  # W2: product catalogue (+ staff CRUD)
├── review/                   # W7: review submission + moderation
└── shared/
    ├── ApiErrorResponse.java
    ├── GlobalExceptionHandler.java
    ├── InvalidTokenException.java    # W8: maps to 401
    ├── RequestIdFilter.java          # W9: UUID per-request in MDC + X-Request-Id header
    ├── RateLimitingFilter.java       # W9: Bucket4j rate limits (auth: 20/10min, reviews: 5/hr)
    └── ...
├── email/                            # W10: transactional emails
│   ├── OrderConfirmedEvent.java      # Spring event published after order commit
│   └── OrderEmailService.java        # @TransactionalEventListener + @Async sender
└── admin/                            # W11: staff-only listing endpoints
    ├── AdminOrderController.java     # GET /v1/admin/orders
    ├── AdminOrderService.java
    ├── AdminReviewController.java    # GET /v1/admin/reviews
    ├── AdminReviewService.java
    ├── AdminContactController.java   # GET /v1/admin/contact-submissions
    └── AdminContactService.java
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
