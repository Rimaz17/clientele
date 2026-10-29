package dev.rimaz.clientele.stats;

import dev.rimaz.clientele.AbstractTestcontainers;
import dev.rimaz.clientele.customer.Gender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Runs the stats queries against a real PostgreSQL database with known rows.
 * The stats cover the whole table, so every test starts from an empty one;
 * this is safe because test classes run one after another.
 */
class CustomerStatsJDBCDataAccessServiceTest extends AbstractTestcontainers {

    private final JdbcTemplate jdbcTemplate = getJdbcTemplate();
    private final CustomerStatsJDBCDataAccessService underTest =
            new CustomerStatsJDBCDataAccessService(jdbcTemplate);

    @BeforeEach
    void emptyTheTable() {
        jdbcTemplate.update("DELETE FROM customer");
    }

    @Test
    void totalsOfAnEmptyTableAreZeroWithoutAnAverage() {
        CustomerTotals totals = underTest.selectTotals(Instant.parse("2026-01-01T00:00:00Z"));

        assertThat(totals).isEqualTo(new CustomerTotals(0, 0, null));
        assertThat(underTest.countByGender()).isEmpty();
        assertThat(underTest.countByAgeRange()).isEmpty();
        assertThat(underTest.countSignupsByMonth(Instant.parse("2026-01-01T00:00:00Z"))).isEmpty();
    }

    @Test
    void countsEveryCustomerAndThoseCreatedSinceAnInstant() {
        insert(20, Gender.MALE, "2026-03-01T00:00:00Z");
        insert(30, Gender.FEMALE, "2026-03-15T00:00:00Z");
        insert(41, Gender.FEMALE, "2026-04-01T00:00:00Z");

        CustomerTotals totals = underTest.selectTotals(Instant.parse("2026-03-15T00:00:00Z"));

        // The boundary is inclusive; (20 + 30 + 41) / 3 = 30.333... rounds to 30.3.
        assertThat(totals).isEqualTo(new CustomerTotals(3, 2, 30.3));
    }

    @Test
    void countsCustomersByGender() {
        insert(30, Gender.MALE, "2026-03-01T00:00:00Z");
        insert(30, Gender.FEMALE, "2026-03-01T00:00:00Z");
        insert(30, Gender.FEMALE, "2026-03-01T00:00:00Z");

        assertThat(underTest.countByGender())
                .containsExactly(entry(Gender.MALE, 1L), entry(Gender.FEMALE, 2L));
    }

    @Test
    void leavesOutAGenderWithoutCustomers() {
        insert(30, Gender.FEMALE, "2026-03-01T00:00:00Z");

        assertThat(underTest.countByGender()).containsExactly(entry(Gender.FEMALE, 1L));
    }

    @Test
    void putsEveryAgeInTheRightRangeIncludingTheBoundaries() {
        for (int age : new int[]{16, 24, 25, 34, 35, 44, 45, 54, 55, 64, 65, 100}) {
            insert(age, Gender.MALE, "2026-03-01T00:00:00Z");
        }
        insert(20, Gender.MALE, "2026-03-01T00:00:00Z");

        assertThat(underTest.countByAgeRange()).containsExactly(
                entry(AgeRange.AGE_16_TO_24, 3L),
                entry(AgeRange.AGE_25_TO_34, 2L),
                entry(AgeRange.AGE_35_TO_44, 2L),
                entry(AgeRange.AGE_45_TO_54, 2L),
                entry(AgeRange.AGE_55_TO_64, 2L),
                entry(AgeRange.AGE_65_AND_OVER, 2L)
        );
    }

    @Test
    void leavesOutAnAgeRangeWithoutCustomers() {
        insert(70, Gender.MALE, "2026-03-01T00:00:00Z");

        assertThat(underTest.countByAgeRange()).containsExactly(entry(AgeRange.AGE_65_AND_OVER, 1L));
    }

    @Test
    void countsSignUpsPerUtcMonthFromAnInstant() {
        insert(30, Gender.MALE, "2026-01-31T23:59:59Z");
        insert(30, Gender.MALE, "2026-02-01T00:00:00Z");
        insert(30, Gender.MALE, "2026-02-28T12:00:00Z");
        insert(30, Gender.MALE, "2026-04-10T08:00:00Z");

        Map<YearMonth, Long> signups = underTest.countSignupsByMonth(Instant.parse("2026-02-01T00:00:00Z"));

        // January is before the start; March has no sign-ups, so it is absent.
        assertThat(signups).containsOnly(
                entry(YearMonth.of(2026, 2), 2L),
                entry(YearMonth.of(2026, 4), 1L)
        );
    }

    @Test
    void usesTheUtcMonthWhateverTheOffsetOfTheStoredTime() {
        // 23:30 at UTC-05:00 on 31 January is already 1 February in UTC.
        insert(30, Gender.MALE, OffsetDateTime.parse("2026-01-31T23:30:00-05:00"));

        assertThat(underTest.countSignupsByMonth(Instant.parse("2026-01-01T00:00:00Z")))
                .containsOnly(entry(YearMonth.of(2026, 2), 1L));
    }

    private void insert(int age, Gender gender, String createdAt) {
        insert(age, gender, OffsetDateTime.ofInstant(Instant.parse(createdAt), ZoneOffset.UTC));
    }

    private void insert(int age, Gender gender, OffsetDateTime createdAt) {
        jdbcTemplate.update("""
                        INSERT INTO customer(name, email, password, age, gender, created_at)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                "Stats", "stats-" + UUID.randomUUID() + "@example.com", "password", age, gender.name(), createdAt);
    }
}
