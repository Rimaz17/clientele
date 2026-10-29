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

These credentials are for local development only. You can also create a regular account on the sign-up page. The `dev` profile adds a demo customer the first time it starts (not on every start); demo customers use the password `password`.

### Roles

Every account is a regular user (`USER`) unless it is an admin (`ADMIN`). Users can change, delete and upload a picture only for their own account; admins can do this for any account. Every signed-in user can see the customer list. The API enforces these rules and returns `403` otherwise; the React app hides the controls you cannot use.

Outside the `dev` profile there is no admin until you configure one. When both variables are set, startup creates that admin, or promotes the existing account with that email and sets its password to the configured one:

| Variable | Default | Purpose |
|---|---|---|
| `CLIENTELE_BOOTSTRAP_ADMIN_EMAIL` | none (dev profile: `admin@clientele.dev`) | Email of the admin account |
| `CLIENTELE_BOOTSTRAP_ADMIN_PASSWORD` | none (dev profile: `admin-password`) | Its password, at least 8 characters |

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
