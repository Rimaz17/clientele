package dev.rimaz.clientele.customer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerAccessTest {

    private static final int CUSTOMER_ID = 7;

    @Mock
    private CustomerDao customerDao;
    private CustomerAccess underTest;

    @BeforeEach
    void setUp() {
        underTest = new CustomerAccess(customerDao);
    }

    @Test
    void customersAreThemselves() {
        givenCustomerWithEmail("alex@clientele.dev");

        assertThat(underTest.isSelf(CUSTOMER_ID, authenticatedAs("alex@clientele.dev"))).isTrue();
    }

    @Test
    void emailsAreComparedCaseInsensitively() {
        givenCustomerWithEmail("Alex@Clientele.dev");

        assertThat(underTest.isSelf(CUSTOMER_ID, authenticatedAs("alex@clientele.DEV"))).isTrue();
    }

    @Test
    void otherUsersAreNotSelf() {
        givenCustomerWithEmail("alex@clientele.dev");

        assertThat(underTest.isSelf(CUSTOMER_ID, authenticatedAs("jamila@clientele.dev"))).isFalse();
    }

    @Test
    void unknownCustomersAreNotSelf() {
        when(customerDao.selectCustomerById(CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThat(underTest.isSelf(CUSTOMER_ID, authenticatedAs("alex@clientele.dev"))).isFalse();
    }

    @Test
    void missingAuthenticationIsNotSelf() {
        assertThat(underTest.isSelf(CUSTOMER_ID, null)).isFalse();
        verifyNoInteractions(customerDao);
    }

    @Test
    void missingCustomerIdIsNotSelf() {
        assertThat(underTest.isSelf(null, authenticatedAs("alex@clientele.dev"))).isFalse();
        verifyNoInteractions(customerDao);
    }

    private void givenCustomerWithEmail(String email) {
        Customer customer = new Customer(CUSTOMER_ID, "Alex", email, "password", 30, Gender.MALE);
        when(customerDao.selectCustomerById(CUSTOMER_ID)).thenReturn(Optional.of(customer));
    }

    private static Authentication authenticatedAs(String email) {
        return new TestingAuthenticationToken(email, null, "ROLE_USER");
    }
}
