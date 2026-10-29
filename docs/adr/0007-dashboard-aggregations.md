# 0007. Dashboard aggregations in SQL

- **Status:** Accepted
- **Date:** 2026-10-30

## Context

The React `/dashboard` page was empty. It needs headline numbers (total customers, new customers in the last 30 days, average age) and three breakdowns (gender split, age distribution, sign-ups per month over the last year). The customer table had no record of when anyone signed up, and the only way to read customers was the paginated list, so a client could only compute statistics by downloading every page.

## Decision

- **A `created_at` column.** Migration `V4` adds `created_at TIMESTAMPTZ NOT NULL DEFAULT now()` and the index `customer_created_at_idx`. The database is the only writer: inserts leave the column out, the JPA entity maps it with `insertable = false, updatable = false` and `@Generated(GenerationTime.INSERT)` so Hibernate reads the value back after an insert, and the JDBC row mapper reads it like any other column. `TIMESTAMPTZ` stores an instant, so the API returns `createdAt` as an ISO-8601 UTC timestamp. Hibernate 6.1's schema validation expects `timestamp(6) with time zone`, while PostgreSQL reports `timestamptz`; the entity's `columnDefinition = "TIMESTAMPTZ"` makes the two agree.
- **Existing rows are backfilled with the migration time.** There is no true sign-up date for customers created before `V4`, and inventing one would be worse. Every older row therefore counts as signed up in the month the migration ran.
- **Newest first by default.** `createdAt` joins the sort whitelist (column `created_at`), and the list's default sort changes from `id,asc` (ADR 0006) to `createdAt,desc`, which is what staff usually want to see first.
- **`GET /api/v1/customers/stats`.** Any signed-in user may call it; it returns only aggregates, never personal data. The literal path segment `stats` is more specific than `/customers/{customerId}`, so Spring routes it to its own controller; an integration test checks this.
- **Aggregation in SQL.** A read-only `CustomerStatsDao` (JDBC only) answers with four aggregate queries: `count(*)`, `count(*) FILTER (WHERE created_at >= :newSince)` and `round(avg(age), 1)` in one statement; `GROUP BY gender`; `GROUP BY` a `CASE` that buckets ages into 16–24, 25–34, 35–44, 45–54, 55–64 and 65+; and `GROUP BY to_char(date_trunc('month', created_at AT TIME ZONE 'UTC'), 'YYYY-MM')` for sign-ups. No customer row is loaded into the application, so the cost stays a few index or table scans in the database as the table grows.
- **One snapshot.** `CustomerStatsService` runs the four queries in one read-only `REPEATABLE READ` transaction, so a customer who signs up between two queries cannot make the totals disagree with the breakdowns.
- **UTC months, zero-filled in the service.** Months are UTC calendar months, so the chart does not depend on the server's or the database session's time zone. The window is the current UTC month and the 11 before it. SQL only returns groups that have rows; the service fills in every gender, every age range and every month with 0 where nothing matched, so clients always get the same shape. "New in the last 30 days" is a rolling window from the injected `Clock`, which makes both windows testable with a fixed clock.
- **Ages below 16.** The API has accepted only ages 16 to 100 since ADR 0003. Older rows outside that range can only come from before validation existed, and they count in the youngest bucket rather than disappearing from the totals.
- **Demo data.** In the `dev` profile, `DevDataSeeder` backdates each demo customer it creates to a random time in the last 330 days, weighted towards recent months, so the sign-up chart has a history. Real sign-ups are never backdated.
- **React.** The page uses Recharts 2 with the two series colours from the Chakra theme (`chart.series1`, `chart.series2`), validated for colour-blind separation and 3:1 contrast on white. Each chart is one described image for screen readers, with a sentence summarising it, and can switch to a table of the same numbers, so no value is reachable only by hovering. Charts do not animate when the user prefers reduced motion.

## Consequences

- The dashboard reads one small JSON document whatever the table size, and the numbers always come from real records.
- Customers created before `V4` share one sign-up month. On a long-lived database that month shows a spike; this is documented rather than hidden.
- A dev database that already had its 50 demo customers before this change keeps them in the migration month; only newly seeded demo customers are spread out.
- The stats queries scan the whole table. That is fine for thousands of rows; the `created_at` index serves the date filters. With millions of rows, caching or a materialized view would be the next step.
- `CustomerStatsDao` has a JDBC implementation only, unlike `CustomerDao`. It is read-only reporting SQL, and a JPA version would only wrap the same native queries.

## Alternatives considered

- **Computing the stats in Java from all customers**: simple, but it loads every row (and every password hash) into memory on each request, and the cost grows with the table.
- **Zero-filling months in SQL with `generate_series`**: one query fewer in Java, but the SQL gets harder to read and test, and genders and age ranges would still need filling. Filling in the service keeps one rule for all three.
- **Months in the server's local time zone**: matches a single office's calendar, but the result would change with server configuration. UTC is predictable, and the response says which months it covers.
- **Setting `created_at` in the application**: lets the seeder write it directly, but two code paths (JDBC and JPA) would have to agree on the clock, and an insert that forgot it would fail or store null. A database default cannot be forgotten.
- **A separate stats table updated on every sign-up**: constant-time reads, but more writes and a second source of truth to keep consistent, which this scale does not need.
