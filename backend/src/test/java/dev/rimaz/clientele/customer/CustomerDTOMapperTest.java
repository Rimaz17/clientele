package dev.rimaz.clientele.customer;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerDTOMapperTest {

    private final CustomerDTOMapper underTest = new CustomerDTOMapper();

    @Test
    void mapsEveryPublicFieldIncludingCreatedAt() {
        Instant createdAt = Instant.parse("2026-03-14T09:26:53Z");
        Customer customer = new Customer(7, "Alex", "alex@clientele.dev", "secret-hash", 30,
                Gender.MALE, "image-id", Role.ADMIN, createdAt);

        CustomerDTO actual = underTest.apply(customer);

        assertThat(actual).isEqualTo(new CustomerDTO(
                7,
                "Alex",
                "alex@clientele.dev",
                Gender.MALE,
                30,
                List.of("ROLE_ADMIN"),
                "alex@clientele.dev",
                "image-id",
                createdAt
        ));
    }
}
