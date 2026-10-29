# 0005. Role-based access and ownership

- **Status:** Accepted
- **Date:** 2026-10-29

## Context

Any authenticated user could update, delete or replace the profile picture of any customer. Every account got a hard-coded `ROLE_USER` authority, so the backend had no way to tell an administrator from a regular user. The React app showed edit and delete buttons on every card, which suggested the actions were allowed.

Two related problems came with it:

- The JWT subject is the email. Users who changed their own email kept a token naming an account that no longer existed, so their next request failed.
- `Main` inserted a random customer on every startup, in every profile, so the table grew on each restart and production would have received fake data.

## Decision

- **Role model.** A `Role` enum (`USER`, `ADMIN`) stored in a `role` column on `customer` (migration `V3`), `NOT NULL DEFAULT 'USER'` with the named check constraint `customer_role_check`. `Customer.getAuthorities()` returns `ROLE_<role>`, and the JWT `scopes` claim carries the same value.
- **A column, not a table.** Each account has exactly one of two roles, and there is no role management UI. A `customer_role` join table would add a join to every authentication and a second DAO for no current benefit. If accounts ever need several roles or fine-grained permissions, a new migration can move the column into a table without changing the authorities the rest of the code sees.
- **Ownership rule.** "Admins may change anyone; everyone else only themselves." `CustomerAccess.isSelf(customerId, authentication)` compares the authenticated email with the customer's email, case-insensitively. `CustomerService` enforces it with method security (`@EnableMethodSecurity`):

  ```java
  @PreAuthorize("hasRole('ADMIN') or @customerAccess.isSelf(#customerId, authentication)")
  ```

  on `updateCustomer`, `deleteCustomerById` and `uploadCustomerProfileImage`. Reads stay open to every authenticated user, and the profile-image `GET` stays public (an `<img>` tag cannot send a token). A denied call raises `AccessDeniedException`, which `DefaultExceptionHandler` already maps to a `403` `ApiError`. For a non-admin, an unknown id is also `403`, so the response does not reveal whether the id exists.
- **The rule lives in the service, not the controller.** Any future caller of the service, not only today's controller, gets the same check.
- **No privilege fields in requests.** `CustomerRegistrationRequest` and `CustomerUpdateRequest` have no `role` or `id` field, and Jackson ignores unknown JSON properties, so a `"role": "ADMIN"` in a sign-up body has no effect. Sign-up always creates a `USER`. An integration test proves this.
- **Stale token after an email change.** When users change their own email, the `PUT` response carries a new token in the `Authorization` header. The controller compares the authenticated principal's id with the updated customer and checks whether the email changed. Admins editing someone else get no token. The old token is rejected: the JWT filter cannot load its subject, so the request stays unauthenticated and gets `403`, not `500`.
- **Admin bootstrap.** `AdminBootstrap` (an `ApplicationRunner`) reads `clientele.bootstrap.admin.email` and `.password` (`CLIENTELE_BOOTSTRAP_ADMIN_EMAIL` and `CLIENTELE_BOOTSTRAP_ADMIN_PASSWORD`). With both set, it creates the admin, or promotes the existing account **and resets its password to the configured one**. Without the reset, anyone who registered that email before the first deployment would become an admin with a password they chose. It logs customer ids only, never the email or password. The `dev` profile configures a dev-only admin; no other environment gets one unless the variables are set.
- **Dev seeding.** `DevDataSeeder` replaces the runner in `Main`. It runs only in the `dev` profile with `clientele.seed.enabled=true`, and tops the table up to a fixed number of regular users instead of adding one on every start. The integration tests share the local database, so they switch it off through the `@IntegrationTest` annotation.
- **UI.** React reads `isAdmin` from the token scopes and the current email from `sub`. It shows Update and Delete only on the user's own card or to admins, marks admins and the user's own card with badges, stores a replacement token from the `PUT` response, explains a `403` in the toast, and signs users out after they delete their own account. The Angular app shows a clear message on `403` and stores the replacement token.

## Consequences

- The API, not the UI, decides who may change what. Hiding buttons is only a convenience, and a direct API call gets the same `403`.
- `isSelf` costs one primary-key lookup per protected write, which is negligible here.
- Role changes take effect on the backend at the next request, because the filter loads authorities from the database. The frontends read roles from the token, so they show a new role only after the next login.
- Production has no admin until an operator sets the two environment variables. Restarting with them set is idempotent.
- The configured admin password overrides the account's password on every start where they differ.
- There are no endpoints to grant or revoke roles; promoting another user means changing the bootstrap configuration or the database. Role management endpoints are future work.

## Alternatives considered

- **Checks inside the service methods** (`if (!isAdmin && !isSelf) throw ...`): explicit, but repeated in every method and easy to forget on a new one. The `@PreAuthorize` rule is declarative and testable through the API.
- **URL rules in the security filter chain**: `requestMatchers` cannot compare the path id with the caller, so ownership would still need code elsewhere.
- **Comparing user ids instead of emails** in `isSelf`: it avoids one lookup, but depends on the principal being a `Customer`. Comparing emails works with any `Authentication` and matches how tokens identify users.
- **A separate `customer_role` table**: rejected for now, see above.
- **Promoting the first registered user to admin**: whoever signs up first in a new environment would control it.
