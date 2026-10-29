# Architecture

Clientele is a single Spring Boot REST API with two browser clients. The API is stateless: every protected request carries a JWT in the `Authorization` header.

## System overview

```mermaid
flowchart LR
    subgraph Clients
        React["React app<br/>(Vite, Chakra UI)"]
        Angular["Angular app<br/>(PrimeNG)"]
    end

    subgraph API["Spring Boot API"]
        Filter["JWT authentication filter<br/>(Spring Security)"]
        Controllers["Controllers<br/>auth, customers, ping"]
        Services["Services<br/>AuthenticationService, CustomerService, S3Service"]
        DAO["CustomerDao<br/>JDBC and JPA implementations"]
    end

    DB[("PostgreSQL<br/>schema managed by Flyway")]
    S3[("Amazon S3<br/>or FakeS3 on local disk")]

    React -- "HTTPS + JWT" --> Filter
    Angular -- "HTTPS + JWT" --> Filter
    Filter --> Controllers
    Controllers --> Services
    Services --> DAO
    DAO --> DB
    Services -- "profile images" --> S3
```

## Layers

The backend is an N-tier application, organised by feature package (`auth`, `customer`, `exception`, `jwt`, `s3`, `security`).

| Layer | Responsibility |
|---|---|
| Security filter chain | `JWTAuthenticationFilter` validates the bearer token and sets the authenticated user. Public endpoints are registration, login, `/ping`, profile-image downloads and actuator health. |
| Controllers | HTTP only: request mapping, status codes and response headers. |
| Services | Business rules, such as duplicate email checks and update validation, and authorization: `@PreAuthorize` rules on `CustomerService` (see below). |
| DAO | `CustomerDao` hides persistence. `CustomerService` uses the JDBC implementation (`JdbcTemplate`); authentication uses the JPA implementation (Spring Data `CustomerRepository`). |
| Database | PostgreSQL. Flyway owns the schema, and Hibernate only validates it. |
| File storage | `S3Service` stores profile images in S3. When `aws.s3.mock` is `true`, `FakeS3` writes them to the local disk instead. |

Errors are thrown as domain exceptions and turned into `ApiError` responses in one place, `DefaultExceptionHandler`.

## Authentication flow

```mermaid
sequenceDiagram
    participant UI as React or Angular
    participant API as Spring Boot API
    participant DB as PostgreSQL

    UI->>API: POST /api/v1/auth/login (email, password)
    API->>DB: load customer by email
    API-->>UI: 200, JWT in the Authorization header
    UI->>API: GET /api/v1/customers (Authorization: Bearer JWT)
    API->>API: JWT filter validates the token
    API->>DB: query customers
    API-->>UI: 200, customer list
```

Tokens are signed with HS256 using the secret in `clientele.jwt.secret` and carry the configured issuer and expiry (`JwtProperties`). The `dev` profile, active by default, supplies a development-only secret; every other environment sets `CLIENTELE_JWT_SECRET` or the API does not start. An invalid, expired or foreign token leaves the request unauthenticated, so protected endpoints answer 403.

## Authorization

Every account has a role, `USER` or `ADMIN`, stored in the `customer.role` column and exposed as the `ROLE_USER` or `ROLE_ADMIN` authority and in the JWT `scopes` claim.

| Action | Who may do it |
|---|---|
| List and view customers | Any signed-in user |
| Download a profile image | Anyone (public, because `<img>` tags cannot send a token) |
| Update, delete, upload a profile image | Admins, or the customer themselves |

`CustomerService` enforces the last row with `@PreAuthorize("hasRole('ADMIN') or @customerAccess.isSelf(#customerId, authentication)")`. A denied call is answered with a `403` `ApiError`. When users change their own email, the `PUT` response carries a new token, because the old one names the old email. On startup, `AdminBootstrap` creates or promotes the configured admin, and in the `dev` profile `DevDataSeeder` adds demo customers. See [ADR 0005](adr/0005-role-based-access-and-ownership.md).

## Customer list queries

`GET /api/v1/customers` binds its query string to a validated `CustomerQuery` (page, size, sort, search, gender, age range) and returns a `PageResponse`. The JDBC DAO builds the `WHERE` clause from named parameters, orders by a column from the `CustomerSortField` whitelist with an `id` tie-breaker, and runs a matching `COUNT(*)`. The JPA DAO does the same with specifications and a `Pageable`. Search text is escaped so `%` and `_` match literally. See [ADR 0006](adr/0006-offset-pagination-and-search.md).

## Deployment

The backend image is built with Jib and the React image with its Dockerfile. `docker-compose.yml` runs PostgreSQL, the API and the React app together. `Dockerrun.aws.json` describes an Elastic Beanstalk deployment; the deployment workflow only runs when triggered manually.
