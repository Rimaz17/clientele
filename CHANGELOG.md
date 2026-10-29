# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- ESLint, Prettier, Vitest and React Testing Library for the React app, with a Login smoke test.
- Frontend CI workflow that lints, tests and builds the React app and builds the Angular app.
- `.env.example` files documenting the environment variables, plus `.editorconfig` and `.gitattributes`.
- Architecture overview, architecture decision records, and pull request and issue templates.
- Server-side validation of registration, update and login requests. Invalid input returns `400` with an `errors` list of `{ field, message }` entries in the `ApiError` body.
- Profile picture uploads must be non-empty JPEG, PNG or WebP files.
- The React customer forms show server-side field errors under the matching inputs.
- Customer roles: every account is a `USER` or an `ADMIN`, stored in a new `role` column (migration `V3`) and carried in the JWT `scopes` claim.
- Admin bootstrap: when `CLIENTELE_BOOTSTRAP_ADMIN_EMAIL` and `CLIENTELE_BOOTSTRAP_ADMIN_PASSWORD` are set, startup creates that admin or promotes the existing account. The `dev` profile includes a development-only admin (`admin@clientele.dev`).
- When users change their own email, the update response carries a new token, and both frontends store it so the user stays signed in.
- The React customer cards mark admins and the signed-in user's own card, and the header shows the role as Admin or User.
- The customer list can be paged, searched, filtered and sorted: `GET /api/v1/customers?page=0&size=20&sort=name,asc&search=ali&gender=FEMALE&minAge=18&maxAge=40`. Search matches name or email, ignoring case; invalid parameters return `400` with field errors.
- The React customers page has a debounced search box, gender and age filters, a sort selector, page numbers with a page-size selector and a "Showing 21–40 of 132" summary. All of it is kept in the URL, so refresh, shared links and the back button work.
- The React customers page tells an empty database apart from a search with no results, which offers "Clear filters".
- Customers have a sign-up time: a `created_at` column (migration `V4`, set by the database) returned as `createdAt` in UTC. Customers created before the migration get the time it ran.
- `GET /api/v1/customers/stats` returns dashboard statistics computed with SQL aggregations: total customers, new customers in the last 30 days, average age, the gender split, six age ranges and sign-ups per UTC month for the last 12 months, with every group present.
- The React dashboard shows three stat cards and charts of monthly sign-ups, the gender split and the age distribution, with loading placeholders, an error state with a retry and an empty state. Every chart has a screen reader summary and a table view of its numbers.
- Interactive API documentation: Swagger UI at `/swagger-ui/index.html` and the OpenAPI document at `/v3/api-docs`, both public. Every endpoint lists its parameters, request and response bodies with examples, and the errors it can return; protected endpoints can be called after pasting a token into **Authorize**. Set `SPRINGDOC_SWAGGERUI_ENABLED=false` to turn Swagger UI off.

### Changed
- **BREAKING:** `GET /api/v1/customers` returns a page object, `{ content, page, size, totalElements, totalPages }`, instead of a JSON array. Both frontends are updated; other clients must read `content`.
- The customer list is no longer silently cut off at 1000 rows.
- The `dev` profile keeps 50 demo customers, so the list has several pages.
- Backend CI runs on every pull request to `main`, with Maven caching and least-privilege permissions.
- Backend deployment runs only when triggered manually.
- Maven artifact, Docker images and compose services are renamed to `clientele`.
- The React `.env` file is no longer tracked; copy `frontend/react/.env.example` instead.
- The API's `.env.example` moved from the repository root to `backend/.env.example`, next to the code that reads it. For docker compose, still copy it to `.env` in the repository root.
- The backend base package is now `dev.rimaz.clientele` and the Maven `groupId` is `dev.rimaz`.
- Demo customers are seeded only in the `dev` profile (`clientele.seed.enabled`), and only until a fixed number exists, instead of one new random customer on every start in every profile.
- The React app shows Update and Delete only on the signed-in user's own card, or on every card for admins, and signs users out after they delete their own account.
- A `403` on a change now says the user can only change their own account, in both frontends.
- The customer list is sorted newest first by default (`createdAt,desc` instead of `id,asc`), and `createdAt` can be used in `sort`. The React sort selector offers "Newest first" and "Oldest first" instead of the id-based options.
- Demo customers in the `dev` profile get sign-up dates spread over the past year.
- The React sidebar's first link is now "Dashboard", the header shows the Clientele name instead of the course logo and a "Logo" placeholder, and the browser tab is titled Clientele.

### Removed
- The unused in-memory customer DAO.
- React controls that did nothing: the sidebar "Settings" link, the user menu's "Profile", "Settings" and "Billing" items, and the header bell button.

### Fixed
- Registering or updating with an email that is already taken returns `409 Conflict` instead of `500`.
- An update that changes nothing returns `400 Bad Request` instead of `500`.
- Malformed JSON, a non-numeric customer id and a missing upload part return `400`; an unsupported method returns `405`, an unsupported content type `415`, and an oversized upload `413`.
- Unexpected errors return `500` with a generic message and no internal details; the full error is logged on the server.
- An invalid or expired bearer token returns the usual `403` error body without a server-side stack trace.
- The React sign-up and customer forms show a correct message for an invalid email, and the age message no longer says 100 is too old.
- React form labels are linked to their inputs, and error toasts still appear when the API is unreachable.
- React form errors are announced by screen readers (linked to their input with `aria-invalid` and `aria-describedby`), required fields are marked, and error text meets WCAG AA contrast.
- A taken email is shown under the email field, and error toasts have readable titles instead of HTTP client codes.
- The profile picture dropzone has an accessible name and focus ring, lists the accepted types and the 10 MB limit, and explains rejected files; a customer without a picture shows their initials.
- The Angular app shows the API error message when registering, saving, updating or deleting a customer fails.
- React sidebar links show a keyboard focus ring, mark the current page, keep AA contrast on hover and navigate without reloading the app.
- `docker compose` now creates the `customer` database when the volume is first created.
- Testcontainers tests work with Docker Engine 29 and later.

### Security
- The JWT signing secret is no longer hard-coded. It is read from `clientele.jwt.secret` (environment variable `CLIENTELE_JWT_SECRET`); the `dev` profile, now the default, supplies a development-only secret. Any other profile refuses to start without a secret of at least 32 bytes. The previous secret is in Git history and is treated as compromised, so existing tokens stop working and users must log in again.
- Only admins and the customer themselves can update or delete a customer or upload their profile picture; anyone else gets `403`. Reading customers stays open to every signed-in user.
- Sign-up always creates a regular user; a `role` field in the request body is ignored.
- A token whose user no longer exists, for example after an email change or a deletion, is rejected with `403`.
- The JWT issuer and lifetime are configurable (`clientele.jwt.issuer`, default `clientele`; `clientele.jwt.expiration`, default `15d`), and tokens from another issuer are rejected.
