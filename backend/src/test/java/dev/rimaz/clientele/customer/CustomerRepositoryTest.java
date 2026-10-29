package dev.rimaz.clientele.customer;

import dev.rimaz.clientele.AbstractTestcontainers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CustomerRepositoryTest extends AbstractTestcontainers {

    @Autowired
    private CustomerRepository underTest;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private TestEntityManager entityManager;

    @BeforeEach
    void setUp() {
        underTest.deleteAll();
        System.out.println(applicationContext.getBeanDefinitionCount());
    }

    @Test
    void existsCustomerByEmail() {
        // Given
        String email = FAKER.internet().safeEmailAddress() + "-" + UUID.randomUUID();
        Customer customer = new Customer(
                FAKER.name().fullName(),
                email,
                "password",
                20,
                Gender.MALE);

        underTest.save(customer);

        // When
        var actual = underTest.existsCustomerByEmail(email);

        // Then
        assertThat(actual).isTrue();
    }

    @Test
    void existsCustomerByEmailFailsWhenEmailNotPresent() {
        // Given
        String email = FAKER.internet().safeEmailAddress() + "-" + UUID.randomUUID();

        // When
        var actual = underTest.existsCustomerByEmail(email);

        // Then
        assertThat(actual).isFalse();
    }

    @Test
    void existsCustomerById() {
        // Given
        String email = FAKER.internet().safeEmailAddress() + "-" + UUID.randomUUID();
        Customer customer = new Customer(
                FAKER.name().fullName(),
                email,
                "password", 20,
                Gender.MALE);

        underTest.save(customer);

        int id = underTest.findAll()
                .stream()
                .filter(c -> c.getEmail().equals(email))
                .map(Customer::getId)
                .findFirst()
                .orElseThrow();

        // When
        var actual = underTest.existsCustomerById(id);

        // Then
        assertThat(actual).isTrue();
    }

    @Test
    void existsCustomerByIdFailsWhenIdNotPresent() {
        // Given
        int id = -1;

        // When
        var actual = underTest.existsCustomerById(id);

        // Then
        assertThat(actual).isFalse();
    }

    @Test
    void canUpdateProfileImageId() {
        // Given
        String email = "email";

        Customer customer = new Customer(
                FAKER.name().fullName(),
                email,
                "password", 20,
                Gender.MALE);

        underTest.save(customer);

        int id = underTest.findAll()
                .stream()
                .filter(c -> c.getEmail().equals(email))
                .map(Customer::getId)
                .findFirst()
                .orElseThrow();

        // When
        underTest.updateProfileImageId("2222", id);

        // Then
        Optional<Customer> customerOptional = underTest.findById(id);
        assertThat(customerOptional)
                .isPresent()
                .hasValueSatisfying(
                        c -> assertThat(c.getProfileImageId()).isEqualTo("2222")
                );
    }

    @Test
    void savesAndLoadsTheRole() {
        // Given
        String email = FAKER.internet().safeEmailAddress() + "-" + UUID.randomUUID();
        Customer customer = new Customer(
                null,
                FAKER.name().fullName(),
                email,
                "password", 20,
                Gender.FEMALE,
                null,
                Role.ADMIN);

        // When
        underTest.saveAndFlush(customer);
        // Forget the cached entity so the role is read back from the database.
        entityManager.clear();

        // Then
        assertThat(underTest.findCustomerByEmail(email))
                .hasValueSatisfying(c -> assertThat(c.getRole()).isEqualTo(Role.ADMIN));
    }

    @Test
    void theDatabaseSetsCreatedAtOnInsert() {
        // Given
        String email = FAKER.internet().safeEmailAddress() + "-" + UUID.randomUUID();
        Customer customer = new Customer(
                FAKER.name().fullName(),
                email,
                "password", 20,
                Gender.MALE);

        // When
        Customer saved = underTest.saveAndFlush(customer);
        entityManager.clear();

        // Then
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(underTest.findCustomerByEmail(email))
                .hasValueSatisfying(c -> assertThat(c.getCreatedAt()).isEqualTo(saved.getCreatedAt()));
    }

    @Test
    void updatesNeverChangeCreatedAt() {
        // Given
        String email = FAKER.internet().safeEmailAddress() + "-" + UUID.randomUUID();
        Customer saved = underTest.saveAndFlush(new Customer(
                FAKER.name().fullName(),
                email,
                "password", 20,
                Gender.MALE));
        entityManager.clear();
        Customer detached = new Customer(saved.getId(), "Renamed", email, "password", 21,
                Gender.MALE, null, Role.USER, null);

        // When
        underTest.saveAndFlush(detached);
        entityManager.clear();

        // Then
        assertThat(underTest.findById(saved.getId())).hasValueSatisfying(c -> {
            assertThat(c.getName()).isEqualTo("Renamed");
            assertThat(c.getCreatedAt()).isEqualTo(saved.getCreatedAt());
        });
    }

    @Test
    void countsCustomersByRole() {
        // Given
        Customer user = new Customer(
                FAKER.name().fullName(),
                FAKER.internet().safeEmailAddress() + "-" + UUID.randomUUID(),
                "password", 20,
                Gender.MALE);
        Customer admin = new Customer(
                null,
                FAKER.name().fullName(),
                FAKER.internet().safeEmailAddress() + "-" + UUID.randomUUID(),
                "password", 20,
                Gender.FEMALE,
                null,
                Role.ADMIN);
        underTest.saveAll(List.of(user, admin));

        // When
        long users = underTest.countByRole(Role.USER);
        long admins = underTest.countByRole(Role.ADMIN);

        // Then
        assertThat(users).isEqualTo(1);
        assertThat(admins).isEqualTo(1);
    }
}
