package dev.rimaz.clientele.stats;

import dev.rimaz.clientele.customer.Gender;

import java.util.List;
import java.util.Map;

/**
 * The dashboard's customer statistics. Only aggregates, never personal data.
 *
 * @param totalCustomers  every customer
 * @param newLast30Days   customers who signed up in the last 30 days
 * @param averageAge      the average age to one decimal, or {@code null} without customers
 * @param genderSplit     customers per gender, with every gender present
 * @param ageDistribution customers per age range, youngest first, with every range present
 * @param signupsByMonth  sign-ups in each of the last 12 UTC months, oldest first, with every month present
 */
public record CustomerStats(
        long totalCustomers,
        long newLast30Days,
        Double averageAge,
        Map<Gender, Long> genderSplit,
        List<AgeRangeCount> ageDistribution,
        List<MonthlySignups> signupsByMonth
) {
}
