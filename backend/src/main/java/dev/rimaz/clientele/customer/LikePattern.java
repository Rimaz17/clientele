package dev.rimaz.clientele.customer;

/**
 * Builds LIKE patterns from user text. The text is still sent as a bound
 * parameter; escaping only stops {@code %} and {@code _} from acting as
 * wildcards, so a search for "50%" finds "50%" and not "50 anything".
 */
final class LikePattern {

    /** The escape character used in the SQL and JPA LIKE expressions. */
    static final char ESCAPE = '\\';

    private LikePattern() {
    }

    /** A pattern that matches values containing the text anywhere. */
    static String contains(String text) {
        String escaped = text
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
