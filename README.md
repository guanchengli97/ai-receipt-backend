# AI Receipt Backend

Spring Boot backend for AI Receipt, a receipt management service that supports user authentication, image uploads, AI-powered receipt parsing, receipt analytics, and Stripe billing.

## Tech Stack

- Java 17
- Spring Boot 2.7.12
- Spring Web, Spring Security, Spring Data JPA, Spring Mail
- MySQL 8
- JWT authentication
- Google ID token login
- Gemini API for receipt extraction
- AWS S3 for receipt image storage
- Stripe for checkout, billing portal, subscriptions, and webhooks
- Docker, Docker Compose, GitHub Actions, and optional Ansible MySQL provisioning

## Project Structure

```text
.
├── src/main/java/com/example/aireceiptbackend
│   ├── config/        # Security, JWT filter, AWS S3, REST client config
│   ├── controller/    # Auth, users, receipts, images, billing, health APIs
│   ├── exception/     # Domain exceptions
│   ├── model/         # JPA entities and request/response DTOs
│   ├── repository/    # Spring Data repositories
│   ├── service/       # Auth, receipt parsing, image storage, email, billing logic
│   └── util/          # JWT utilities
├── src/main/resources/application.yml
├── ansible/           # MySQL provisioning for Oracle/Linux hosts
├── .github/workflows/deploy.yml
├── Dockerfile         # Runtime image for CI/CD builds
├── Dockerfile.local   # Multi-stage local Docker build
├── docker-compose.yml # Local MySQL + app stack
└── pom.xml
```

## Configuration

Copy the example environment file and fill in local values:

```bash
cp .env.example .env
```

Important variables:

| Variable | Purpose |
| --- | --- |
| `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD` | MySQL database and credentials |
| `JWT_SECRET` | JWT signing secret |
| `GOOGLE_CLIENT_ID` | Google OAuth client ID for Google login |
| `MAIL_PASSWORD` | SMTP password for activation emails |
| `ACTIVATION_LINK_BASE` | Base URL used in email activation links |
| `GEMINI_API_KEY` | Gemini API key for receipt parsing |
| `AWS_S3_BUCKET`, `AWS_S3_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` | S3 image storage |
| `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET` | Stripe API and webhook verification |
| `STRIPE_PRICE_PRO_MONTHLY`, `STRIPE_PRICE_PRO_YEARLY` | Stripe price IDs for paid plans |
| `DAILY_RECEIPT_SCAN_LIMIT_FREE`, `DAILY_RECEIPT_SCAN_LIMIT_PRO` | Receipt parsing limits by plan |

Do not commit real secrets. Keep production values in GitHub Actions secrets or server-side environment variables.

## Run Locally

### Option 1: Docker Compose

Docker Compose starts MySQL and the Spring Boot app together:

```bash
docker compose up --build
```

The API will be available at:

```text
http://localhost:8080/api
```

### Option 2: Maven

Start a MySQL 8 database first, then provide Spring datasource settings through environment variables or a local configuration override.

```bash
mvn spring-boot:run
```

Build the application JAR:

```bash
mvn clean package
```

Run the packaged JAR:

```bash
java -jar target/ai-receipt-backend-0.0.1-SNAPSHOT.jar
```

## API Overview

The application uses the `/api` context path. Protected endpoints require:

```text
Authorization: Bearer <jwt-token>
```

### Health

- `GET /api/` - basic service status
- `GET /api/api/health` - health check route currently defined by the application

### Authentication

- `POST /api/auth/register` - register a user and send an activation email
- `GET /api/auth/activate?token=...` - activate email address
- `POST /api/auth/login` - login with email and password
- `POST /api/auth/google` - login with a Google ID token

Login request:

```json
{
  "email": "user@example.com",
  "password": "password"
}
```

Login response:

```json
{
  "token": "<jwt-token>"
}
```

### Users

- `GET /api/users/me` - current user profile
- `PUT /api/users/me` - update current user profile
- `GET /api/users/{username}` - get a user by username
- `PUT /api/users/{username}` - update a user by username

### Images

- `POST /api/images/upload-url` - create a presigned S3 upload URL
- `GET /api/images/{id}/presigned-url` - create a presigned S3 download URL

### Receipts

- `POST /api/receipts/parse` - parse and save a receipt from an uploaded image ID
- `GET /api/receipts/me` - list current user's receipts
- `GET /api/receipts/me/range?start=YYYY-MM-DD&end=YYYY-MM-DD` - list receipts by date range
- `GET /api/receipts/me/stats` - monthly receipt statistics
- `GET /api/receipts/me/stats/by-category` - monthly category spending statistics
- `GET /api/receipts/{id}` - get a receipt
- `PUT /api/receipts/{id}` - update receipt details
- `PUT /api/receipts/{id}/review` - update review status
- `DELETE /api/receipts/{id}` - delete one receipt
- `DELETE /api/receipts` - delete multiple receipts

### Billing

- `POST /api/billing/checkout-session` - create a Stripe checkout session
- `POST /api/billing/portal-session` - create a Stripe billing portal session
- `GET /api/billing/me` - current user's billing status
- `GET /api/billing/me/usage` - current user's receipt usage
- `POST /api/billing/webhook` - Stripe webhook endpoint

## Deployment

The primary application deployment path is Docker-based CI/CD:

1. Push to the `master` branch.
2. GitHub Actions builds the Java application.
3. The workflow builds and pushes a multi-architecture Docker image to Docker Hub.
4. The workflow connects to the Oracle host over SSH.
5. The host pulls the latest image and restarts the `ai-receipt-app` container.

Production container port mapping in the workflow:

```text
host 7008 -> container 8080
```

MySQL provisioning is handled separately through the `ansible/` directory. The Ansible setup is intended for database/server initialization, while the Java application itself is deployed as a Docker container.

Useful deployment docs:

- `QUICK_START.md`
- `ORACLE_DEPLOYMENT.md`
- `ORACLE_QUICK_REFERENCE.md`
- `ANSIBLE_DEPLOYMENT.md`
- `DEPLOYMENT_CHECKLIST.md`

## Notes

- JPA is configured with `hibernate.ddl-auto=update`, so schema changes are applied automatically at runtime.
- The application context path is `/api`; include it when calling endpoints.
- `application.yml` contains template-style placeholders for deployed environments. For local development, prefer Docker Compose or override datasource and secret values through environment-specific configuration.
- Review `.env.example` before sharing the repository and ensure it contains placeholders only, not real credentials.
