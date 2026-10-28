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

### Changed
- Backend CI runs on every pull request to `main`, with Maven caching and least-privilege permissions.
- Backend deployment runs only when triggered manually.
- Maven artifact, Docker images and compose services are renamed to `clientele`.
- The React `.env` file is no longer tracked; copy `frontend/react/.env.example` instead.

### Removed
- The unused in-memory customer DAO.

### Fixed
- `docker compose` now creates the `customer` database when the volume is first created.
- Testcontainers tests work with Docker Engine 29 and later.
