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

### Changed
- Backend CI runs on every pull request to `main`, with Maven caching and least-privilege permissions.
- Backend deployment runs only when triggered manually.
- Maven artifact, Docker images and compose services are renamed to `clientele`.
- The React `.env` file is no longer tracked; copy `frontend/react/.env.example` instead.
- The backend base package is now `dev.rimaz.clientele` and the Maven `groupId` is `dev.rimaz`.

### Removed
- The unused in-memory customer DAO.

### Fixed
- Registering or updating with an email that is already taken returns `409 Conflict` instead of `500`.
- An update that changes nothing returns `400 Bad Request` instead of `500`.
- Malformed JSON, a non-numeric customer id and a missing upload part return `400`; an unsupported method returns `405`, an unsupported content type `415`, and an oversized upload `413`.
- Unexpected errors return `500` with a generic message and no internal details; the full error is logged on the server.
- An invalid or expired bearer token returns the usual `403` error body without a server-side stack trace.
- The React sign-up and customer forms show a correct message for an invalid email, and the age message no longer says 100 is too old.
- React form labels are linked to their inputs, and error toasts still appear when the API is unreachable.
- The Angular app shows the API error message when registering, saving, updating or deleting a customer fails.
- `docker compose` now creates the `customer` database when the volume is first created.
- Testcontainers tests work with Docker Engine 29 and later.
