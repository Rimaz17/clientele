package dev.rimaz.clientele.bootstrap;

import dev.rimaz.clientele.customer.Customer;
import dev.rimaz.clientele.customer.CustomerRepository;
import dev.rimaz.clientele.customer.Gender;
import dev.rimaz.clientele.customer.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapTest {

    private static final String EMAIL = "admin@clientele.dev";
    private static final String PASSWORD = "admin-password";
    private static final String PASSWORD_HASH = "hashed-admin-password";
    private static final String OTHER_HASH = "hash-of-another-password";

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void createsTheAdminWhenTheAccountIsMissing() {
        when(customerRepository.findCustomerByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_HASH);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        run(properties(EMAIL, PASSWORD));

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        Customer admin = captor.getValue();
        assertThat(admin.getEmail()).isEqualTo(EMAIL);
        assertThat(admin.getPassword()).isEqualTo(PASSWORD_HASH);
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getName()).isEqualTo("Admin");
        assertThat(admin.getAge()).isEqualTo(30);
        assertThat(admin.getGender()).isEqualTo(Gender.MALE);
    }

    @Test
    void promotesAnExistingUserAndResetsTheirPassword() {
        Customer existing = new Customer(7, "Alex", EMAIL, OTHER_HASH, 30, Gender.FEMALE);
        when(customerRepository.findCustomerByEmail(EMAIL)).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches(PASSWORD, OTHER_HASH)).thenReturn(false);
        when(passwordEncoder.encode(PASSWORD)).thenReturn(PASSWORD_HASH);

        run(properties(EMAIL, PASSWORD));

        verify(customerRepository).save(existing);
        assertThat(existing.getRole()).isEqualTo(Role.ADMIN);
        assertThat(existing.getPassword()).isEqualTo(PASSWORD_HASH);
        assertThat(existing.getName()).isEqualTo("Alex");
    }

    @Test
    void promotesAnExistingUserWhosePasswordAlreadyMatches() {
        Customer existing = new Customer(7, "Alex", EMAIL, PASSWORD_HASH, 30, Gender.FEMALE);
        when(customerRepository.findCustomerByEmail(EMAIL)).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);

        run(properties(EMAIL, PASSWORD));

        verify(customerRepository).save(existing);
        verify(passwordEncoder, never()).encode(any());
        assertThat(existing.getRole()).isEqualTo(Role.ADMIN);
        assertThat(existing.getPassword()).isEqualTo(PASSWORD_HASH);
    }

    @Test
    void doesNothingWhenTheAdminAlreadyExists() {
        Customer admin = new Customer(1, "Admin", EMAIL, PASSWORD_HASH, 30, Gender.MALE, null, Role.ADMIN);
        when(customerRepository.findCustomerByEmail(EMAIL)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);

        run(properties(EMAIL, PASSWORD));

        verify(customerRepository, never()).save(any());
    }

    @Test
    void doesNothingWhenNotConfigured() {
        run(properties(null, null));

        verifyNoInteractions(customerRepository, passwordEncoder);
    }

    @Test
    void doesNothingWhenOnlyTheEmailIsSet() {
        run(properties(EMAIL, " "));

        verifyNoInteractions(customerRepository, passwordEncoder);
    }

    private void run(AdminBootstrapProperties properties) {
        new AdminBootstrap(properties, customerRepository, passwordEncoder)
                .run(new DefaultApplicationArguments());
    }

    private static AdminBootstrapProperties properties(String email, String password) {
        return new AdminBootstrapProperties(email, password, "Admin", 30, Gender.MALE);
    }
}
