# 0002. Error handling strategy

- **Status:** Accepted
- **Date:** 2026-10-29

## Context

The API had one `@ControllerAdvice`, `DefaultExceptionHandler`, with handlers for 404, 401 and 403 and a catch-all handler for `Exception`. The domain exceptions `DuplicateResourceException` and `RequestValidationException` carry `@ResponseStatus(409)` and `@ResponseStatus(400)`, but Spring picks the most specific `@ExceptionHandler` first, so the catch-all handler won and both came back as **500**. Framework errors such as malformed JSON, a non-numeric path variable or a missing upload part were also reported as 500.

The catch-all handler also returned the raw exception message, which can contain SQL, class names or file paths, and `server.error.include-message` was set to `always`.

Both frontends read the `message` field of the `ApiError` body, so the response shape must not change.

## Decision

- Keep **one** `@ControllerAdvice`, `DefaultExceptionHandler`, as the only place that turns exceptions into HTTP responses.
- Add an **explicit handler per exception type**, each returning the existing `ApiError` shape (`path`, `message`, `statusCode`, `localDateTime`):

  | Exception | Status |
  |---|---|
  | `ResourceNotFoundException` | 404 |
  | `DuplicateResourceException` | 409 |
  | `RequestValidationException` | 400 |
  | `HttpMessageNotReadableException` | 400 |
  | `MethodArgumentTypeMismatchException` | 400 |
  | `MissingServletRequestParameterException`, `MissingServletRequestPartException` | 400 |
  | `HttpRequestMethodNotSupportedException` | 405 |
  | `HttpMediaTypeNotSupportedException` | 415 |
  | `MaxUploadSizeExceededException` | 413 |
  | `BadCredentialsException` | 401 |
  | `InsufficientAuthenticationException`, `AccessDeniedException` | 403 |

- Framework exceptions get **fixed, user-friendly messages** built from safe fields (such as the parameter name) instead of the framework's own message.
- The catch-all handler **logs** the exception with its stack trace at `ERROR` and returns 500 with the generic message `An unexpected error occurred`.
- Spring's fallback `/error` response is configured to include no message, stack trace, exception name or binding errors.
- An invalid, expired or wrongly signed bearer token leaves the request unauthenticated instead of throwing from the JWT filter, so protected endpoints answer with the usual 403 `ApiError`.

## Consequences

- Clients get the correct status code for every expected error, and the `message` field stays meaningful for the frontends.
- No internal detail reaches clients through a 500. Operators find the details in the logs instead.
- Each new domain exception needs its own handler. Forgetting one shows up as a 500 in tests, which makes the gap visible.
- Unit tests cover each handler, and integration tests (`ErrorHandlingIT`) cover the main journeys end to end.

## Alternatives considered

- **Rely on `@ResponseStatus` on the exceptions:** does not work while a catch-all handler exists, and gives no control over the body shape.
- **Extend `ResponseEntityExceptionHandler`:** handles most framework exceptions automatically, but in Spring 6 it renders RFC 7807 `ProblemDetail` bodies. Overriding it to keep `ApiError` adds more code than the explicit handlers, and would change the shape the frontends depend on.
- **Remove the catch-all handler:** unexpected errors would then fall through to Spring's default error page, which has a different body shape.
