package dev.rimaz.clientele.stats;

import io.swagger.v3.oas.annotations.media.Schema;

/** How many customers signed up in one UTC calendar month, written as {@code "2026-03"}. */
public record MonthlySignups(
        @Schema(description = "A UTC calendar month, as YYYY-MM", example = "2026-03")
        String month,
        @Schema(example = "8")
        long count
) {
}
