package dev.rimaz.clientele.stats;

/**
 * Whole-table customer counts.
 *
 * @param total      every customer
 * @param newSince   customers created at or after the requested instant
 * @param averageAge the average age rounded to one decimal, or {@code null} when there are no customers
 */
public record CustomerTotals(long total, long newSince, Double averageAge) {
}
