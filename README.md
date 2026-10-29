# Clientele

Clientele is a full-stack customer management system. Users sign up, log in with a JWT and manage customer records: list, view, create, update, delete and upload profile pictures.

One Spring Boot REST API serves two frontends: a React app (the primary UI) and an Angular app.

## Tech stack

| Area | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3, Spring Security 6, JWT (jjwt) |
| Data | PostgreSQL 14, Flyway, Spring Data JPA, Spring JDBC |
| Files | AWS S3 SDK v2, with a local-disk fake for development |
| React | React 18, Vite 4, Chakra UI, Formik + Yup, Axios, React Router 6 |
| Angular | Angular 15, PrimeNG |
| Tests | JUnit 5, Mockito, AssertJ, Testcontainers, Vitest, React Testing Library |
| Delivery | Docker, docker-compose, Jib, GitHub Actions |

## Run it locally

Prerequisites: Java 17, Maven, Node 18 or 20, and Docker Desktop (running).

### 1. Database

```bash
docker compose up -d db
```

PostgreSQL listens on `localhost:5332` and creates the `customer` database the first time its volume is created. If you already had a volume from before that setting, create the database once:

```bash
docker exec postgres psql -U amigoscode -c "CREATE DATABASE customer;"
```

### 2. Backend

```bash
cd backend
mvn spring-boot:run
```

The API runs on `http://localhost:8080`. Flyway applies the migrations on startup.

Local runs use the `dev` profile by default, which includes a development-only JWT secret, so no setup is needed.

### JWT secret outside development

Any profile other than `dev` (including the docker-compose API container and the AWS deployment) needs a JWT signing secret of at least 32 bytes in the `CLIENTELE_JWT_SECRET` environment variable. Without it, the API refuses to start and names the missing property.

Generate a secret:

```bash
openssl rand -base64 48
```

Set it in bash:

```bash
export CLIENTELE_JWT_SECRET="<generated value>"
```

Set it in PowerShell:

```powershell
$env:CLIENTELE_JWT_SECRET = "<generated value>"
```

For docker-compose you can instead copy `.env.example` to `.env` and replace the placeholder. Never commit `.env`. For the AWS deployment, set `CLIENTELE_JWT_SECRET` as an environment property of the Elastic Beanstalk environment.

| Variable | Default | Purpose |
|---|---|---|
| `CLIENTELE_JWT_SECRET` | none (dev profile only: built-in dev secret) | Signs and verifies JWTs |
| `CLIENTELE_JWT_EXPIRATION` | `15d` | Token lifetime |
| `CLIENTELE_JWT_ISSUER` | `clientele` | `iss` claim written to and required on tokens |

### 3. React app

```bash
cd frontend/react
cp .env.example .env
npm ci
npm run dev
```

Open `http://localhost:5173`.

### Logging in

In development (the `dev` profile) the API creates an admin account on startup:

| Email | Password | Role |
|---|---|---|
| `admin@clientele.dev` | `admin-password` | Admin |

These credentials are for local development only. You can also create a regular account on the sign-up page. The `dev` profile tops the database up to 50 demo customers when it starts with fewer, so the list has several pages; restarting does not add more. Demo customers use the password `password`, and their sign-up dates are spread over the past year so the dashboard has a history.

### Roles

Every account is a regular user (`USER`) unless it is an admin (`ADMIN`). Users can change, delete and upload a picture only for their own account; admins can do this for any account. Every signed-in user can see the customer list. The API enforces these rules and returns `403` otherwise; the React app hides the controls you cannot use.

Outside the `dev` profile there is no admin until you configure one. When both variables are set, startup creates that admin, or promotes the existing account with that email and sets its password to the configured one:

| Variable | Default | Purpose |
|---|---|---|
| `CLIENTELE_BOOTSTRAP_ADMIN_EMAIL` | none (dev profile: `admin@clientele.dev`) | Email of the admin account |
| `CLIENTELE_BOOTSTRAP_ADMIN_PASSWORD` | none (dev profile: `admin-password`) | Its password, at least 8 characters |

## API

The REST API lives under `/api/v1`. Every endpoint except sign-up (`POST /customers`), login (`POST /auth/login`) and profile-image downloads needs an `Authorization: Bearer <token>` header.

### Interactive API docs

With the backend running, open Swagger UI at [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html). It lists every endpoint with its parameters, request and response bodies and error codes, and lets you call them. The raw OpenAPI document is at `http://localhost:8080/v3/api-docs`. Neither page needs a token.

To call a protected endpoint from Swagger UI:

1. Open **Authentication**, choose `POST /api/v1/auth/login`, then **Try it out**, and log in, for example with the development admin from [Logging in](#logging-in).
2. Copy the `token` value from the response.
3. Choose **Authorize** at the top of the page, paste the token without the `Bearer ` prefix, and confirm.
4. Protected endpoints, marked with a lock, now send the token.

Swagger UI is on by default. Set `SPRINGDOC_SWAGGERUI_ENABLED=false` to turn it off; the OpenAPI document stays available.

The customer list is paginated:

```
GET /api/v1/customers?page=0&size=20&sort=name,asc&search=ali&gender=FEMALE&minAge=18&maxAge=40
```

| Parameter | Rules | Default |
|---|---|---|
| `page` | 0 or more | `0` |
| `size` | 1 to 100 | `20` |
| `sort` | `id`, `name`, `email`, `age` or `createdAt`, optionally followed by `,asc` or `,desc` | `createdAt,desc` |
| `search` | matches name or email, ignoring case; at most 100 characters | none |
| `gender` | `MALE` or `FEMALE` | none |
| `minAge`, `maxAge` | 16 to 100, inclusive; `minAge` must not be greater than `maxAge` | none |

The response is `{ "content": [...], "page": 0, "size": 20, "totalElements": 132, "totalPages": 7 }`. Invalid parameters return `400` with an `errors` list. Each customer includes `createdAt`, the sign-up time in UTC.

The dashboard reads aggregate statistics, computed in SQL:

```
GET /api/v1/customers/stats
```

```json
{
  "totalCustomers": 132,
  "newLast30Days": 14,
  "averageAge": 37.4,
  "genderSplit": { "MALE": 70, "FEMALE": 62 },
  "ageDistribution": [
    { "range": "16-24", "count": 20 },
    { "range": "25-34", "count": 41 },
    { "range": "35-44", "count": 33 },
    { "range": "45-54", "count": 19 },
    { "range": "55-64", "count": 12 },
    { "range": "65+", "count": 7 }
  ],
  "signupsByMonth": [
    { "month": "2025-11", "count": 8 },
    { "month": "2025-12", "count": 5 }
  ]
}
```

`signupsByMonth` always has 12 entries, the current UTC month and the 11 before it, oldest first (shortened above). `averageAge` is `null` when there are no customers. Every gender, age range and month is present, with `0` when empty.

## Tests

```bash
# Backend: unit, Testcontainers and integration tests (Docker must be running)
cd backend && mvn -ntp verify

# React: lint, tests and production build
cd frontend/react && npm run lint && npm test && npm run build
```

## Documentation

- [Architecture](docs/architecture.md)
- [Architecture decision records](docs/adr/)
- [Changelog](CHANGELOG.md)
