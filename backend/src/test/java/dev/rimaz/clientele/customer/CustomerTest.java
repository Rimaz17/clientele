package dev.rimaz.clientele.customer;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerTest {

    @Test
    void newCustomersAreRegularUsers() {
        Customer customer = new Customer("Alex", "alex@clientele.dev", "password", 30, Gender.MALE);

        assertThat(customer.getRole()).isEqualTo(Role.USER);
        assertThat(customer.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    void authoritiesReflectTheRole() {
        Customer customer = new Customer(1, "Alex", "alex@clientele.dev", "password", 30,
                Gender.MALE, null, Role.ADMIN);

        assertThat(customer.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }
}
