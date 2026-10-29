package dev.rimaz.clientele.stats;

import dev.rimaz.clientele.customer.Gender;

import java.time.Instant;
import java.time.YearMonth;
import java.util.Map;

/**
 * Read-only aggregations over the customer table for the dashboard. Every
 * method answers with one aggregate query, so no customer rows are loaded.
 * Groups without customers are left out of the returned maps.
 */
public interface CustomerStatsDao {
    CustomerTotals selectTotals(Instant newSince);
    Map<Gender, Long> countByGender();
    Map<AgeRange, Long> countByAgeRange();
    /** Sign-ups per calendar month in UTC, counting customers created at or after {@code from}. */
    Map<YearMonth, Long> countSignupsByMonth(Instant from);
}
