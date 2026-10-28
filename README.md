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

### 3. React app

```bash
cd frontend/react
cp .env.example .env
npm ci
npm run dev
```

Open `http://localhost:5173`.

### Logging in

Create an account on the sign-up page. In development the API also adds a random customer on every start; its email is printed in the backend log and its password is `password`.

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
