package dev.rimaz.clientele.stats;

import dev.rimaz.clientele.customer.Gender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerStatsServiceTest {

    // 01:30 in UTC on 2 March 2026, still 1 March in New York.
    private static final Instant NOW = Instant.parse("2026-03-02T01:30:00Z");

    @Mock
    private CustomerStatsDao customerStatsDao;

    private CustomerStatsService underTest;

    @BeforeEach
    void setUp() {
        underTest = new CustomerStatsService(customerStatsDao, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void passesTheTotalsThroughAndCountsNewCustomersFromThirtyDaysAgo() {
        when(customerStatsDao.selectTotals(Instant.parse("2026-01-31T01:30:00Z")))
                .thenReturn(new CustomerTotals(132, 14, 37.4));
        stubEmptyBreakdowns();

        CustomerStats stats = underTest.getCustomerStats();

        assertThat(stats.totalCustomers()).isEqualTo(132);
        assertThat(stats.newLast30Days()).isEqualTo(14);
        assertThat(stats.averageAge()).isEqualTo(37.4);
    }

    @Test
    void reportsEveryGenderWithZeroForMissingOnes() {
        stubTotals();
        when(customerStatsDao.countByGender()).thenReturn(Map.of(Gender.FEMALE, 62L));
        when(customerStatsDao.countByAgeRange()).thenReturn(Map.of());
        when(customerStatsDao.countSignupsByMonth(any())).thenReturn(Map.of());

        assertThat(underTest.getCustomerStats().genderSplit())
                .containsExactly(entry(Gender.MALE, 0L), entry(Gender.FEMALE, 62L));
    }

    @Test
    void reportsEveryAgeRangeYoungestFirstWithZeroForMissingOnes() {
        stubTotals();
        when(customerStatsDao.countByGender()).thenReturn(Map.of());
        when(customerStatsDao.countByAgeRange()).thenReturn(Map.of(
                AgeRange.AGE_65_AND_OVER, 3L,
                AgeRange.AGE_25_TO_34, 20L
        ));
        when(customerStatsDao.countSignupsByMonth(any())).thenReturn(Map.of());

        assertThat(underTest.getCustomerStats().ageDistribution()).containsExactly(
                new AgeRangeCount("16-24", 0),
                new AgeRangeCount("25-34", 20),
                new AgeRangeCount("35-44", 0),
                new AgeRangeCount("45-54", 0),
                new AgeRangeCount("55-64", 0),
                new AgeRangeCount("65+", 3)
        );
    }

    @Test
    void reportsTheLastTwelveUtcMonthsOldestFirstWithZeroForQuietMonths() {
        stubTotals();
        when(customerStatsDao.countByGender()).thenReturn(Map.of());
        when(customerStatsDao.countByAgeRange()).thenReturn(Map.of());
        when(customerStatsDao.countSignupsByMonth(Instant.parse("2025-04-01T00:00:00Z"))).thenReturn(Map.of(
                YearMonth.of(2025, 4), 5L,
                YearMonth.of(2026, 3), 2L
        ));

        List<MonthlySignups> signups = underTest.getCustomerStats().signupsByMonth();

        assertThat(signups).hasSize(12);
        assertThat(signups.get(0)).isEqualTo(new MonthlySignups("2025-04", 5));
        assertThat(signups.get(1)).isEqualTo(new MonthlySignups("2025-05", 0));
        assertThat(signups.get(10)).isEqualTo(new MonthlySignups("2026-02", 0));
        assertThat(signups.get(11)).isEqualTo(new MonthlySignups("2026-03", 2));
        verify(customerStatsDao).countSignupsByMonth(Instant.parse("2025-04-01T00:00:00Z"));
    }

    @Test
    void anEmptyDatabaseStillHasEveryGroup() {
        when(customerStatsDao.selectTotals(any())).thenReturn(new CustomerTotals(0, 0, null));
        stubEmptyBreakdowns();

        CustomerStats stats = underTest.getCustomerStats();

        assertThat(stats.averageAge()).isNull();
        assertThat(stats.genderSplit()).containsOnly(entry(Gender.MALE, 0L), entry(Gender.FEMALE, 0L));
        assertThat(stats.ageDistribution()).hasSize(6).allSatisfy(range -> assertThat(range.count()).isZero());
        assertThat(stats.signupsByMonth()).hasSize(12).allSatisfy(month -> assertThat(month.count()).isZero());
    }

    private void stubTotals() {
        when(customerStatsDao.selectTotals(any())).thenReturn(new CustomerTotals(1, 1, 30.0));
    }

    private void stubEmptyBreakdowns() {
        when(customerStatsDao.countByGender()).thenReturn(Map.of());
        when(customerStatsDao.countByAgeRange()).thenReturn(Map.of());
        when(customerStatsDao.countSignupsByMonth(any())).thenReturn(Map.of());
    }
}
