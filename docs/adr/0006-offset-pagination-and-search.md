# 0006. Offset pagination and search for the customer list

- **Status:** Accepted; the default sort was changed by [ADR 0007](0007-dashboard-aggregations.md)
- **Date:** 2026-10-29

## Context

`GET /api/v1/customers` returned every customer as a JSON array, silently cut off at 1000 rows by a `LIMIT 1000` in the JDBC query (and `Pageable.ofSize(1000)` in the JPA one). Clients had no way to ask for a page, search or filter, and no way to know that rows were missing. The React app rendered all of them at once.

## Decision

- **Offset pagination.** The endpoint takes `page` (from 0, default 0) and `size` (1 to 100, default 20), and the SQL uses `LIMIT :limit OFFSET :offset` plus a separate `COUNT(*)` with the same `WHERE` clause.
- **A stable response contract.** The response is the project's own `PageResponse` record, `{ content, page, size, totalElements, totalPages }`, not Spring Data's `Page` JSON, which exposes internals (`pageable`, `sort`, `numberOfElements` and others) that can change between versions. This is a breaking change; both frontends were updated in the same change.
- **Filters.** `search` (at most 100 characters, trimmed, blank means none) matches `name` or `email` case-insensitively with `ILIKE`; `gender` is `MALE` or `FEMALE`; `minAge` and `maxAge` are inclusive, each 16 to 100, and `minAge` must not exceed `maxAge`. Filters combine with `AND`.
- **Validation at the boundary.** The parameters bind to a `CustomerQuery` record validated with Bean Validation, including a class-level `@ValidAgeRange` rule that reports on `maxAge`. Invalid values, including ones that cannot be converted, such as `page=abc` or `gender=OTHER`, return `400` with field errors through the existing handler. Conversion errors say "Invalid value" instead of Spring's message, which names Java types.
- **Sort whitelist.** `sort` is `field` or `field,direction`, where the field is one of `id`, `name`, `email`, `age` (the `CustomerSortField` enum) and the direction is `asc` or `desc`; default `id,asc`. The SQL column comes from the enum, never from the request. A request value outside the whitelist fails validation, so text from the request can never reach the `ORDER BY` clause. Every sort except `id` adds `id ASC` as a tie-breaker, so rows with equal values keep a stable order across pages.
- **Search escaping.** The search text is always a bound parameter. In addition, `\`, `%` and `_` are escaped (`LikePattern`) and the queries declare `ESCAPE '\'`, so `50%` matches the literal text "50%" instead of acting as a wildcard. The JPA implementation (specifications) lower-cases both sides because JPQL has no `ILIKE`.
- **Both DAOs.** The JDBC implementation, used by `CustomerService`, builds the `WHERE` clause from named parameters (`NamedParameterJdbcTemplate`). The JPA implementation uses `JpaSpecificationExecutor` with `CustomerSpecifications` and a `PageRequest`. One shared Testcontainers suite runs against both.
- **React.** The list state lives in the URL (`?search=ali&gender=FEMALE&sort=name,asc&page=2`; the URL counts pages from 1 for people). Search and age inputs are debounced by 300 ms. Changing anything except the page returns to page 1. A page past the end, for example after deleting the last customer on the last page, moves to the new last page. The Angular app, which has no paging UI, requests one page of 100.

## Consequences

- No endpoint returns an unbounded list, and clients always know the total.
- `OFFSET` makes the database read and discard the skipped rows, so deep pages get slower as the table grows. At this project's scale (hundreds or thousands of rows) that is negligible.
- Rows inserted or deleted between two requests shift the pages, so a user paging through can see a customer twice or miss one. That is acceptable for an admin list.
- Each list request runs two queries, the page and the count.
- No indexes were added. With a few thousand rows a sequential scan is fast. `ILIKE '%text%'` cannot use a B-tree index because of the leading wildcard; if search becomes slow on a large table, add the `pg_trgm` extension with GIN trigram indexes on `name` and `email` in a new migration (`CREATE INDEX ... USING gin (name gin_trgm_ops)`), and B-tree indexes on `age` or `gender` only if those filters show up in slow-query logs.

## Alternatives considered

- **Keyset (cursor) pagination** (`WHERE (name, id) > (:lastName, :lastId) ORDER BY name, id LIMIT :size`): constant cost for any depth and no shifting pages, but no "jump to page 7", no simple total page count, and a cursor per sort order. The UI needs numbered pages, so offset pagination fits better here.
- **Returning Spring Data's `Page` directly**: less code, but an unstable, framework-shaped contract.
- **Passing `sort` straight to `Sort.by(...)` or into SQL**: invites injection or errors on unknown properties; the enum whitelist is explicit and documented.
- **Full-text search (`tsvector`)**: better for long text and ranking, but names and emails are short, and partial matches such as "ali" in "Alice" are what staff expect.
