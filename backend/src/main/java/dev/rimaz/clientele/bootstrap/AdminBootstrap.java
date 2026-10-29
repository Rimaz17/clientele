package dev.rimaz.clientele.bootstrap;

import dev.rimaz.clientele.customer.Customer;
import dev.rimaz.clientele.customer.CustomerRepository;
import dev.rimaz.clientele.customer.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Makes sure the configured admin account exists on startup.
 * <p>
 * A missing account is created. An existing account is promoted to admin and
 * given the configured password, so an account registered earlier under the
 * same email by someone else cannot become an admin with its own password.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AdminBootstrapProperties properties;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrap(AdminBootstrapProperties properties,
                          CustomerRepository customerRepository,
                          PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isConfigured()) {
            if (properties.isPartiallyConfigured()) {
                log.warn("Admin bootstrap skipped: set both clientele.bootstrap.admin.email "
                        + "and clientele.bootstrap.admin.password");
            }
            return;
        }
        customerRepository.findCustomerByEmail(properties.email())
                .ifPresentOrElse(this::promote, this::createAdmin);
    }

    private void promote(Customer customer) {
        boolean passwordMatches = passwordEncoder.matches(properties.password(), customer.getPassword());
        if (customer.getRole() == Role.ADMIN && passwordMatches) {
            return;
        }
        customer.setRole(Role.ADMIN);
        if (!passwordMatches) {
            customer.setPassword(passwordEncoder.encode(properties.password()));
        }
        customerRepository.save(customer);
        log.info("Promoted customer {} to admin", customer.getId());
    }

    private void createAdmin() {
        Customer admin = new Customer(
                null,
                properties.name(),
                properties.email(),
                passwordEncoder.encode(properties.password()),
                properties.age(),
                properties.gender(),
                null,
                Role.ADMIN
        );
        Customer saved = customerRepository.save(admin);
        log.info("Created admin account {}", saved.getId());
    }
}
