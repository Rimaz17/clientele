package dev.rimaz.clientele.stats;

import dev.rimaz.clientele.customer.Gender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class CustomerStatsService {

    static final Duration NEW_CUSTOMER_WINDOW = Duration.ofDays(30);
    static final int SIGNUP_MONTHS = 12;

    private final CustomerStatsDao customerStatsDao;
    private final Clock clock;

    public CustomerStatsService(CustomerStatsDao customerStatsDao, Clock clock) {
        this.customerStatsDao = customerStatsDao;
        this.clock = clock;
    }

    /**
     * Builds the dashboard statistics. The queries run in one repeatable-read
     * transaction, so they all see the same snapshot and the totals agree with
     * the breakdowns. Genders, age ranges and months without customers are
     * reported with a count of 0, so clients always get the same shape.
     */
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public CustomerStats getCustomerStats() {
        Instant now = clock.instant();
        YearMonth currentMonth = YearMonth.from(now.atOffset(ZoneOffset.UTC));
        YearMonth firstMonth = currentMonth.minusMonths(SIGNUP_MONTHS - 1);
        Instant firstMonthStart = firstMonth.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        CustomerTotals totals = customerStatsDao.selectTotals(now.minus(NEW_CUSTOMER_WINDOW));
        return new CustomerStats(
                totals.total(),
                totals.newSince(),
                totals.averageAge(),
                genderSplit(customerStatsDao.countByGender()),
                ageDistribution(customerStatsDao.countByAgeRange()),
                signupsByMonth(firstMonth, customerStatsDao.countSignupsByMonth(firstMonthStart))
        );
    }

    private static Map<Gender, Long> genderSplit(Map<Gender, Long> counts) {
        Map<Gender, Long> split = new EnumMap<>(Gender.class);
        for (Gender gender : Gender.values()) {
            split.put(gender, counts.getOrDefault(gender, 0L));
        }
        return Collections.unmodifiableMap(split);
    }

    private static List<AgeRangeCount> ageDistribution(Map<AgeRange, Long> counts) {
        return Arrays.stream(AgeRange.values())
                .map(range -> new AgeRangeCount(range.label(), counts.getOrDefault(range, 0L)))
                .toList();
    }

    private static List<MonthlySignups> signupsByMonth(YearMonth firstMonth, Map<YearMonth, Long> counts) {
        return Stream.iterate(firstMonth, month -> month.plusMonths(1))
                .limit(SIGNUP_MONTHS)
                .map(month -> new MonthlySignups(month.toString(), counts.getOrDefault(month, 0L)))
                .toList();
    }
}
