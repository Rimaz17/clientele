package dev.rimaz.clientele.bootstrap;

import dev.rimaz.clientele.customer.Customer;
import dev.rimaz.clientele.customer.CustomerRepository;
import dev.rimaz.clientele.customer.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevDataSeederTest {

    private static final String PASSWORD_HASH = "hashed-password";
    private static final Instant NOW = Instant.parse("2026-10-30T12:00:00Z");

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    private DevDataSeeder underTest;

    @BeforeEach
    void setUp() {
        underTest = new DevDataSeeder(customerRepository, passwordEncoder, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void seedsDemoCustomersWhenThereAreNone() {
        when(customerRepository.countByRole(Role.USER)).thenReturn(0L);
        when(passwordEncoder.encode(DevDataSeeder.DEMO_PASSWORD)).thenReturn(PASSWORD_HASH);
        when(customerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        underTest.run(new DefaultApplicationArguments());

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository, times(DevDataSeeder.DEMO_CUSTOMERS)).save(captor.capture());
        verify(passwordEncoder, times(1)).encode(DevDataSeeder.DEMO_PASSWORD);
        assertThat(captor.getAllValues()).allSatisfy(customer -> {
            assertThat(customer.getRole()).isEqualTo(Role.USER);
            assertThat(customer.getPassword()).isEqualTo(PASSWORD_HASH);
            assertThat(customer.getName()).isNotBlank().hasSizeLessThanOrEqualTo(15);
            assertThat(customer.getEmail()).matches("[a-z]+\\.[a-z]+@example\\.com");
            assertThat(customer.getAge()).isBetween(16, 100);
            assertThat(customer.getGender()).isNotNull();
        });
    }

    @Test
    void spreadsTheSignUpDatesOverThePastMonths() {
        when(customerRepository.countByRole(Role.USER)).thenReturn(0L);
        when(passwordEncoder.encode(DevDataSeeder.DEMO_PASSWORD)).thenReturn(PASSWORD_HASH);
        when(customerRepository.save(any())).thenAnswer(invocation -> {
            Customer customer = invocation.getArgument(0);
            customer.setId(7);
            return customer;
        });

        underTest.run(new DefaultApplicationArguments());

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(customerRepository, times(DevDataSeeder.DEMO_CUSTOMERS))
                .backdateCreatedAt(eq(7), captor.capture());
        assertThat(captor.getAllValues())
                .allSatisfy(createdAt -> assertThat(createdAt)
                        .isBetween(NOW.minus(DevDataSeeder.SIGNUP_SPREAD), NOW));
        assertThat(captor.getAllValues().stream().distinct().count()).isGreaterThan(1);
    }

    @Test
    void doesNotSeedAgainOnRestart() {
        when(customerRepository.countByRole(Role.USER)).thenReturn((long) DevDataSeeder.DEMO_CUSTOMERS);

        underTest.run(new DefaultApplicationArguments());

        verify(customerRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void picksAnotherEmailWhenOneIsTaken() {
        when(customerRepository.countByRole(Role.USER)).thenReturn(DevDataSeeder.DEMO_CUSTOMERS - 1L);
        when(passwordEncoder.encode(DevDataSeeder.DEMO_PASSWORD)).thenReturn(PASSWORD_HASH);
        when(customerRepository.existsCustomerByEmail(anyString())).thenReturn(true, false);
        when(customerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        underTest.run(new DefaultApplicationArguments());

        verify(customerRepository, times(2)).existsCustomerByEmail(anyString());
        verify(customerRepository, times(1)).save(any());
    }
}
