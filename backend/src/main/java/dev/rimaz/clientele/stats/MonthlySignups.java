package dev.rimaz.clientele.stats;

/** How many customers signed up in one UTC calendar month, written as {@code "2026-03"}. */
public record MonthlySignups(String month, long count) {
}
