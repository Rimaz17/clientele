package dev.rimaz.clientele.customer;

import java.util.Arrays;
import java.util.Optional;

/**
 * The whitelist of fields the customer list can be sorted by. SQL only ever
 * sees {@link #column()}, never text from the request.
 */
public enum CustomerSortField {
    ID("id", "id"),
    NAME("name", "name"),
    EMAIL("email", "email"),
    AGE("age", "age");

    // The name used in the API's sort parameter, which is also the JPA attribute.
    private final String property;
    private final String column;

    CustomerSortField(String property, String column) {
        this.property = property;
        this.column = column;
    }

    public String property() {
        return property;
    }

    public String column() {
        return column;
    }

    public static Optional<CustomerSortField> fromProperty(String property) {
        return Arrays.stream(values())
                .filter(field -> field.property.equals(property))
                .findFirst();
    }
}
