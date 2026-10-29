package dev.rimaz.clientele.stats;

import dev.rimaz.clientele.customer.Gender;
import io.swagger.v3.oas.annotations.media.Schema;

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
@Schema(description = "Aggregate customer statistics; no personal data")
public record CustomerStats(
        @Schema(description = "Every customer, admins included", example = "132")
        long totalCustomers,
        @Schema(description = "Customers who signed up in the last 30 days", example = "14")
        long newLast30Days,
        @Schema(description = "Average age to one decimal; null when there are no customers",
                example = "37.4", nullable = true)
        Double averageAge,
        @Schema(description = "Customers per gender, with every gender present",
                example = "{\"MALE\": 70, \"FEMALE\": 62}")
        Map<Gender, Long> genderSplit,
        @Schema(description = "Customers per age range, youngest first: 16-24, 25-34, 35-44, 45-54, 55-64 and 65+")
        List<AgeRangeCount> ageDistribution,
        @Schema(description = "Sign-ups in each of the last 12 UTC months, oldest first")
        List<MonthlySignups> signupsByMonth
) {
}
