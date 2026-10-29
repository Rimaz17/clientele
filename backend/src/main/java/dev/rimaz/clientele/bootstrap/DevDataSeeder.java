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

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Random;

/**
 * Adds demo customers in the dev profile so the UI has data to show.
 * <p>
 * It only tops the table up to {@link #DEMO_CUSTOMERS} regular users, so
 * restarting the application does not add more. Their sign-up dates are spread
 * over the last {@link #SIGNUP_SPREAD}, so the dashboard has a history to
 * chart. Integration tests turn it off with {@code clientele.seed.enabled=false}.
 */
@Component
@Profile("dev")
@ConditionalOnProperty(prefix = "clientele.seed", name = "enabled", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {

    // Enough for a few pages of the customer list at the default page size.
    static final int DEMO_CUSTOMERS = 50;
    static final String DEMO_PASSWORD = "password";
    // Inside the dashboard's 12-month sign-up chart.
    static final Duration SIGNUP_SPREAD = Duration.ofDays(330);

    // Matches the 15-character limit of the sign-up and update forms.
    private static final int MAX_NAME_LENGTH = 15;

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final Faker faker = new Faker();
    private final Random random = new Random();

    public DevDataSeeder(CustomerRepository customerRepository,
                         PasswordEncoder passwordEncoder,
                         Clock clock) {
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
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
            Customer customer = customerRepository.save(randomCustomer(passwordHash));
            customerRepository.backdateCreatedAt(customer.getId(), randomSignupTime());
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

    // 1 - sqrt(u) puts more sign-ups in recent months than in older ones, like a
    // product that is growing.
    private Instant randomSignupTime() {
        double ageFraction = 1 - Math.sqrt(random.nextDouble());
        long ageSeconds = (long) (SIGNUP_SPREAD.toSeconds() * ageFraction);
        return clock.instant().minusSeconds(ageSeconds);
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
