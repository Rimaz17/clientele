# 0003. Input validation at the API boundary

- **Status:** Accepted
- **Date:** 2026-10-29

## Context

Only the React forms validated input. A direct API call could register a customer with a blank name, an invalid email, an age of 5, no gender or a one-character password, and could upload any file as a profile picture. Client-side validation is a convenience for users, not a security control.

## Decision

- Validate every request body at the API boundary with **Jakarta Bean Validation** (`spring-boot-starter-validation`). Constraints live on the request records, and controllers add `@Valid` to each `@RequestBody`.
- The rules **mirror the React forms**, so both layers agree:

  | Record | Field | Rules |
  |---|---|---|
  | `CustomerRegistrationRequest` | `name` | required, at most 15 characters |
  | | `email` | required, valid email, at most 254 characters |
  | | `password` | required, 4 to 15 characters |
  | | `age` | required, 16 to 100 |
  | | `gender` | required |
  | `CustomerUpdateRequest` | `name` | optional; if present, 1 to 15 characters and not blank |
  | | `email` | optional; if present, valid and at most 254 characters |
  | | `age` | optional; if present, 16 to 100 |
  | `AuthenticationRequest` | `username`, `password` | required |

- `MethodArgumentNotValidException` maps to **400** with the usual `ApiError` fields plus an `errors` list of `{ "field", "message" }` entries, sorted by field. Every other error returns an empty `errors` list, so the shape is the same for all responses:

  ```json
  {
    "path": "/api/v1/customers",
    "message": "Request validation failed",
    "statusCode": 400,
    "localDateTime": "2026-10-29T10:15:30",
    "errors": [
      { "field": "age", "message": "Age must be at least 16" },
      { "field": "email", "message": "Email must be a valid email address" }
    ]
  }
  ```

- Profile-picture uploads are checked in `CustomerService`: an empty file, or a content type other than `image/jpeg`, `image/png` or `image/webp`, returns 400 before anything is written to S3.
- The React forms map `errors` onto the matching Formik fields with `setErrors` and show `message` in a toast.

## Consequences

- Invalid data cannot reach the database through the API, whichever client sends it.
- The rules exist in two places (Java annotations and Yup schemas). A change to one must be made in the other; the ADR and the tests make the pairing visible.
- `errors` is a new field. Existing clients ignore it, so the change is not breaking.
- **Known limitation:** passwords are limited to 15 characters because the existing React form enforces that limit. Long passphrases are good practice, so the limit should be raised in both layers in a future change; changing the password policy is out of scope here.
- **Known limitation:** the upload content type is declared by the client. The check catches mistakes, not a deliberately mislabelled file. Inspecting the file's magic bytes would be stronger.
- An unknown `gender` value such as `"OTHER"` fails JSON parsing and returns the generic 400 "Malformed or unreadable request body" instead of a field error.

## Alternatives considered

- **Manual checks in the service layer:** more code, easy to forget a field, and no standard way to report field errors.
- **Validating only in the frontends:** trivially bypassed with a direct API call.
- **A shared JSON Schema for both layers:** removes duplication, but adds tooling that this project does not need yet.
