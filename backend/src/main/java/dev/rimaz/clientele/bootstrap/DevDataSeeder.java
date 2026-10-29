package dev.rimaz.clientele.bootstrap;

import com.github.javafaker.Faker;
import com.github.javafaker.Name;
import dev.rimaz.clientele.customer.Customer;
import dev.rimaz.clientele.customer.CustomerRepository;
import dev.rimaz.clientele.customer.Gender;
import dev.rimaz.clientele.customer.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Random;

/**
 * Adds demo customers in the dev profile so the UI has data to show.
 * <p>
 * It only tops the table up to {@link #DEMO_CUSTOMERS} regular users, so
 * restarting the application does not add more. Integration tests turn it off
 * with {@code clientele.seed.enabled=false}.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "clientele.seed", name = "enabled", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {

    static final int DEMO_CUSTOMERS = 1;
    static final String DEMO_PASSWORD = "password";

    // Matches the 15-character limit of the sign-up and update forms.
    private static final int MAX_NAME_LENGTH = 15;

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final Faker faker = new Faker();
    private final Random random = new Random();

    public DevDataSeeder(CustomerRepository customerRepository,
                         PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        long missing = DEMO_CUSTOMERS - customerRepository.countByRole(Role.USER);
        if (missing <= 0) {
            return;
        }
        // BCrypt is slow on purpose, so every demo customer shares one hash.
        String passwordHash = passwordEncoder.encode(DEMO_PASSWORD);
        for (int i = 0; i < missing; i++) {
            customerRepository.save(randomCustomer(passwordHash));
        }
        log.info("Seeded {} demo customers", missing);
    }

    private Customer randomCustomer(String passwordHash) {
        Name name;
        String email;
        do {
            name = faker.name();
            email = "%s.%s@example.com".formatted(emailPart(name.firstName()), emailPart(name.lastName()));
        } while (customerRepository.existsCustomerByEmail(email));

        return new Customer(
                displayName(name),
                email,
                passwordHash,
                random.nextInt(16, 101),
                random.nextBoolean() ? Gender.MALE : Gender.FEMALE
        );
    }

    private static String displayName(Name name) {
        String fullName = name.firstName() + " " + name.lastName();
        if (fullName.length() <= MAX_NAME_LENGTH) {
            return fullName;
        }
        String firstName = name.firstName();
        return firstName.length() <= MAX_NAME_LENGTH ? firstName : firstName.substring(0, MAX_NAME_LENGTH);
    }

    private static String emailPart(String namePart) {
        return namePart.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
    }
}
