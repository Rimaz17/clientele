package dev.rimaz.clientele.stats;

import io.swagger.v3.oas.annotations.media.Schema;

/** How many customers fall in one age range, such as {@code "25-34"}. */
public record AgeRangeCount(
        @Schema(example = "25-34", allowableValues = {"16-24", "25-34", "35-44", "45-54", "55-64", "65+"})
        String range,
        @Schema(example = "41")
        long count
) {
}
