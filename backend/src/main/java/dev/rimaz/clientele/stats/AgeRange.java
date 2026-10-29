package dev.rimaz.clientele.stats;

import java.util.Arrays;

/**
 * The age buckets of the dashboard's age distribution, youngest first. The
 * labels are the ones the stats SQL returns.
 */
public enum AgeRange {
    AGE_16_TO_24("16-24"),
    AGE_25_TO_34("25-34"),
    AGE_35_TO_44("35-44"),
    AGE_45_TO_54("45-54"),
    AGE_55_TO_64("55-64"),
    AGE_65_AND_OVER("65+");

    private final String label;

    AgeRange(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static AgeRange fromLabel(String label) {
        return Arrays.stream(values())
                .filter(range -> range.label.equals(label))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown age range: " + label));
    }
}
