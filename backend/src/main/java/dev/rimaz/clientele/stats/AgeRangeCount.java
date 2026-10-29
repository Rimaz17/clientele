package dev.rimaz.clientele.stats;

/** How many customers fall in one age range, such as {@code "25-34"}. */
public record AgeRangeCount(String range, long count) {
}
