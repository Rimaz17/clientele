# 0004. Externalized JWT secret

- **Status:** Accepted
- **Date:** 2026-10-29

## Context

`JWTUtil` signed tokens with a secret hard-coded as a Java constant, together with a hard-coded issuer and a 15-day expiry. Anyone who can read the repository could forge a valid token for any user. The same secret was used in every environment, and rotating it meant a code change and a redeploy.

The secret is in the Git history, including the upstream repository this codebase came from. Removing it from the code does not remove it from history.

## Decision

- JWT settings are bound from `clientele.jwt.*` into a validated `JwtProperties` record (`@ConfigurationProperties` + `@Validated`):
  - `secret`: required, and at least 32 bytes, because HS256 needs a 256-bit key and jjwt rejects shorter keys.
  - `expiration`: a `Duration`, `15d` by default (unchanged behaviour).
  - `issuer`: `clientele` by default. Tokens are issued with it and parsing **requires** it.
- `JWTUtil` receives `JwtProperties` through constructor injection and builds the signing key once. No secret remains in `src/main/java`.
- Profiles:
  - `application.yml` holds the issuer and expiration but **no secret**, and sets `spring.profiles.default: dev`.
  - `application-dev.yml` holds a long, random, clearly labelled development-only secret. Because `dev` is the default profile, local runs and all tests work with zero setup.
  - Any explicitly activated profile (the docker-compose `docker` profile, the AWS `test` profile from `Dockerrun.aws.json`, or a future `prod`) does not load the dev file, so it must provide `CLIENTELE_JWT_SECRET`. Spring's relaxed binding maps the variable to `clientele.jwt.secret`.
- Startup **fails fast** with a clear message when the secret is missing or too short. `JwtPropertiesTest` proves this against the real configuration files.
- The old hard-coded secret is treated as **compromised** and is never reused, including as the dev secret.

## Consequences

- Each environment has its own secret, which can be rotated without a code change. Rotating it invalidates all existing tokens, which is the expected effect.
- A misconfigured deployment stops at startup instead of running with a weak or shared secret.
- The dev secret is committed, so it offers no protection. It is only safe because no environment other than `dev` loads it. Anyone activating a profile explicitly gets the fail-fast check.
- Tokens issued before this change (old secret and issuer) are no longer accepted, so users must log in again once.

## Alternatives considered

- **Keep a default secret in `application.yml`:** convenient, but every environment that forgot to override it would silently use a public value.
- **Require the environment variable everywhere, including locally:** safer in principle, but adds setup for every contributor and for CI, and the risk it removes (a known dev secret) only matters if the dev profile reaches a shared environment.
- **Asymmetric keys (RS256) or an external identity provider:** stronger separation between issuing and verifying, but unnecessary while one service both issues and verifies tokens.
- **Rewrite Git history to remove the old secret:** forbidden for this repository's upstream history, and pointless once the secret is public. Rotating it is the correct response.
