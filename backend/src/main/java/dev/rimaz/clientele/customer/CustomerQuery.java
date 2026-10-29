package dev.rimaz.clientele.customer;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Sort;

/**
 * Filters, sort and page of the customer list, bound from the query string of
 * {@code GET /api/v1/customers}. Missing values fall back to the defaults below.
 */
@ValidAgeRange
public record CustomerQuery(
        @Min(value = 0, message = "Page must be 0 or more")
        Integer page,

        @Min(value = 1, message = "Size must be between 1 and 100")
        @Max(value = 100, message = "Size must be between 1 and 100")
        Integer size,

        // Keep in sync with CustomerSortField; CustomerQueryValidationTest checks it.
        @Pattern(
                regexp = "(id|name|email|age)(,(asc|desc))?",
                message = "Sort must be id, name, email or age, optionally followed by ,asc or ,desc"
        )
        String sort,

        @Size(max = 100, message = "Search must be at most 100 characters")
        String search,

        Gender gender,

        @Min(value = 16, message = "Minimum age must be between 16 and 100")
        @Max(value = 100, message = "Minimum age must be between 16 and 100")
        Integer minAge,

        @Min(value = 16, message = "Maximum age must be between 16 and 100")
        @Max(value = 100, message = "Maximum age must be between 16 and 100")
        Integer maxAge
) {

    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final String DEFAULT_SORT = "id,asc";

    public CustomerQuery {
        page = page == null ? DEFAULT_PAGE : page;
        size = size == null ? DEFAULT_SIZE : size;
        sort = sort == null || sort.isBlank() ? DEFAULT_SORT : sort;
        // A blank search means "no search", not "match names containing spaces".
        search = search == null || search.isBlank() ? null : search.strip();
    }

    /** The sort field. Only call this on a query that passed validation. */
    public CustomerSortField sortField() {
        String property = sort.split(",")[0];
        return CustomerSortField.fromProperty(property)
                .orElseThrow(() -> new IllegalStateException("Unvalidated sort field: " + property));
    }

    /** The sort direction, ascending unless {@code desc} is given. */
    public Sort.Direction sortDirection() {
        return sort.endsWith(",desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
    }
}
