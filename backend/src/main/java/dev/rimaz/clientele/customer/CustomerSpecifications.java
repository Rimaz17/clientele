package dev.rimaz.clientele.customer;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The customer list filters as a JPA {@link Specification}, matching the WHERE
 * clause built by the JDBC implementation.
 */
final class CustomerSpecifications {

    private CustomerSpecifications() {
    }

    static Specification<Customer> matching(CustomerQuery query) {
        return (root, criteriaQuery, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query.search() != null) {
                // JPA has no ILIKE, so both sides are lower-cased instead.
                String pattern = LikePattern.contains(query.search().toLowerCase(Locale.ROOT));
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("name")), pattern, LikePattern.ESCAPE),
                        builder.like(builder.lower(root.get("email")), pattern, LikePattern.ESCAPE)
                ));
            }
            if (query.gender() != null) {
                predicates.add(builder.equal(root.get("gender"), query.gender()));
            }
            if (query.minAge() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("age"), query.minAge()));
            }
            if (query.maxAge() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("age"), query.maxAge()));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
