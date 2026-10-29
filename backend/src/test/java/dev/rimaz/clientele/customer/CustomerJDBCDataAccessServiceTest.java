package dev.rimaz.clientele.customer;

import dev.rimaz.clientele.AbstractTestcontainers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerJDBCDataAccessServiceTest extends AbstractTestcontainers {

    private CustomerJDBCDataAccessService underTest;
    private final CustomerRowMapper customerRowMapper = new CustomerRowMapper();

    @BeforeEach
    void setUp() {
        underTest = new CustomerJDBCDataAccessService(
                getJdbcTemplate(),
                customerRowMapper
        );
    }

    @Test
    void selectCustomerById() {
        // Given
        String email = FAKER.internet().safeEmailAddress() + "-" + UUID.randomUUID();
        Customer customer = new Customer(
                FAKER.name().fullName(),
                email,
                "password", 20,
                Gender.MALE);

        underTest.insertCustomer(customer);

        int id = underTest.selectUserByEmail(email)
                .map(Customer::getId)
                .orElseThrow();

        // When
        Optional<Customer> actual = underTest.selectCustomerById(id);

        // Then
        assertThat(actual).isPresent().hasValueSatisfying(c -> {
            assertThat(c.getId()).isEqualTo(id);
            assertThat(c.getName()).isEqualTo(customer.getName());
            assertThat(c.getEmail()).isEqualTo(customer.getEmail());
            assertThat(c.getAge()).isEqualTo(customer.getAge());
        });
    }

    @Test
    void willReturnEmptyWhenSelectCustomerById() {
        // Given
        int id = 0;

        // When
        var actual = underTest.selectCustomerById(id);

        // Then
        assertThat(actual).isEmpty();
    }

    @Test
    void existsPersonWithEmail() {
        // Given
        String email = FAKER.internet().safeEmailAddress() + "-" + UUID.randomUUID();
        String name = FAKER.name().fullName();
        Customer customer = new Customer(
                name,
                email,
                "password", 20,
                Gender.MALE);

        underTest.insertCustomer(customer);

        // When
        boolean actual = underTest.existsCustomerWithEmail(email);

        // Then
        assertThat(actual).isTrue();
    }

    @Test
    void existsPersonWithEmailReturnsFalseWhenDoesNotExists() {
        // Given
        String email = FAKER.internet().safeEmailAddress() + "-" + UUID.randomUUID();

        // When
        boolean actual = underTest.existsCustomerWithEmail(email);

        // Then
        assertThat(actual).isFalse();
    }

    @Test
    void existsCustomerWithId() {
        // Given
        String email = FAKER.internet().safeEmailAddress() + "-" + UUID.randomUUID();
        Customer customer = new Customer(
                FAKER.name().fullName(),
                email,
                "password", 20,
                Gender.MALE);

        underTest.insertCustomer(customer);

        int id = underTest.selectUserByEmail(email)
                .map(Customer::getId)
                .orElseThrow();

        // When
        var actual = underTest.existsCustomerById(id);

        // Then
        assertThat(actual).isTrue();
    }

    @Test
    void existsPersonWithIdWillReturnFalseWhenIdNotPresent() {
        // Given
        int id = -1;

        // When
        var actual = underTest.existsCustomerById(id);

        // Then
        assertThat(actual).isFalse();
    }

    @Test
    void deleteCustomerById() {
        // Given
        String email = FAKER.internet().safeEmailAddress() + "-" + UUID.randomUUID();
        Customer customer = new Customer(
                FAKER.name().fullName(),
                email,
                "password", 20,
                Gender.MALE);

        underTest.insertCustomer(customer);

        int id = underTest.selectUserByEmail(email)
                .map(Customer::getId)
                .orElseThrow();

        // When
        underTest.deleteCustomerById(id);

        // Then
        Optional<Customer> actual = underTest.selectCustomerById(id);
        assertThat(actual).isNotPresent();
    }

    @Test
    void updateCustomerName() {
        // Given
        String email = FAKER.internet().safeEmailAddress() + "-" + UUID.randomUUID();
        Customer customer = new Customer(
                FAKER.name().fullName(),
                email,
                "password", 20,
                Gender.MALE);

        underTest.insertCustomer(customer);

        int id = underTest.selectUserByEmail(email)
                .map(Customer::getId)
                .orElseThrow();

        var newName = "foo";

        // When age is name
        Customer update = new Customer();
        update.setId(id);
        update.setName(newName);

        underTest.updateCustomer(update);

        // Then
        Optional<Customer> actual = underTest.selectCustomerById(id);

        assertThat(actual).isPresent().hasValueSatisfying(c -> {
            assertThat(c.getId()).isEqualTo(id);
            assertThat(c.getName()).isEqualTo(newName); // change
            assertThat(c.getEmail()).isEqualTo(customer.getEmail());
            assertThat(c.getAge()).isEqualTo(customer.getAge());
        });
    }

    @Test
    void updateCustomerEmail() {
        // Given
        String email = FAKER.internet().safeEmailAddress() + "-" + UUID.randomUUID();
        Customer customer = new Customer(
                FAKER.name().fullName(),
                email,
                "password", 20,
                Gender.MALE);

        underTest.insertCustomer(customer);

        int id = underTest.selectUserByEmail(email)
                .map(Customer::getId)
                .orElseThrow();

        var newEmail = FAKER.internet().safeEmailAddress() + "-" + UUID.randomUUID();;

        // When email is changed
        Customer update = new Customer();
        update.setId(id);
        update.setEmail(newEmail);

        underTest.updateCustomer(update);

        // Then
        Optional<Customer> actual = underTest.selectCustomerById(id);

        assertThat(actual).isPresent().hasValueSatisfying(c -> {
            assertThat(c.getId()).isEqualTo(id);
            assertThat(c.getEmail()).isEqualTo(newEmail); // change
            assertThat(c.getName()).isEqualTo(customer.getName());
            assertThat(c.getAge()).isEqualTo(customer.getAge());
        });
    }

    @Test
    void updateCustomerAge() {
        // Given
        String email = FAKER.internet().safeEmailAddress() + "-" + UUID.randomUUID();
        Customer customer = new Customer(
                FAKER.name().fullName(),
                email,
                "password", 20,
                Gender.MALE);

        underTest.insertCustomer(customer);

        int id = underTest.selectUserByEmail(email)
                .map(Customer::getId)
                .orElseThrow();

        var newAge = 100;

        // When age is changed
        Customer update = new Customer();
        update.setId(id);
        update.setAge(newAge);

        underTest.updateCustomer(update);

        // Then
        Optional<Customer> actual = underTest.selectCustomerById(id);

        assertThat(actual).isPresent().hasValueSatisfying(c -> {
            assertThat(c.getId()).isEqualTo(id);
            assertThat(c.getAge()).isEqualTo(newAge); // change
            assertThat(c.getName()).isEqualTo(customer.getName());
            assertThat(c.getEmail()).isEqualTo(customer.getEmail());
        });
    }

    @Test
    void willUpdateAllPropertiesCustomer() {
        // Given
        String email = FAKER.internet().safeEmailAddress() + "-" + UUID.randomUUID();
        Customer customer = new Customer(
                FAKER.name().fullName(),
                email,
                "password", 20,
                Gender.MALE);

        underTest.insertCustomer(customer);

        int id = underTest.selectUserByEmail(email)
                .map(Customer::getId)
                .orElseThrow();

        // When update with new name, age and email
        Customer update = new Customer();
        update.setId(id);
        update.setName("foo");
        String newEmail = UUID.randomUUID().toString();
        update.setEmail(newEmail);
        update.setAge(22);

        underTest.updateCustomer(update);

        // Then
        Optional<Customer> actual = underTest.selectCustomerById(id);

        assertThat(actual).isPresent().hasValueSatisfying(updated -> {
            assertThat(updated.getId()).isEqualTo(id);
            assertThat(updated.getGender()).isEqualTo(Gender.MALE);
            assertThat(updated.getName()).isEqualTo("foo");
            assertThat(updated.getEmail()).isEqualTo(newEmail);
            assertThat(updated.getAge()).isEqualTo(22);
        });
    }

    @Test
    void willNotUpdateWhenNothingToUpdate() {
        // Given
        String email = FAKER.internet().safeEmailAddress() + "-" + UUID.randomUUID();
        Customer customer = new Customer(
                FAKER.name().fullName(),
                email,
                "password", 20,
                Gender.MALE);

        underTest.insertCustomer(customer);

        int id = underTest.selectUserByEmail(email)
                .map(Customer::getId)
                .orElseThrow();

        // When update without no changes
        Customer update = new Customer();
        update.setId(id);

        underTest.updateCustomer(update);

        // Then
        Optional<Customer> actual = underTest.selectCustomerById(id);

        assertThat(actual).isPresent().hasValueSatisfying(c -> {
            assertThat(c.getId()).isEqualTo(id);
            assertThat(c.getAge()).isEqualTo(customer.getAge());
            assertThat(c.getName()).isEqualTo(customer.getName());
            assertThat(c.getEmail()).isEqualTo(customer.getEmail());
        });
    }

    @Test
    void canUpdateProfileImageId() {
        // Given
        String email = FAKER.internet().safeEmailAddress() + "-" + UUID.randomUUID();
        Customer customer = new Customer(
                FAKER.name().fullName(),
                email,
                "password", 20,
                Gender.MALE);

        underTest.insertCustomer(customer);

        int id = underTest.selectUserByEmail(email)
                .map(Customer::getId)
                .orElseThrow();

        // When
        underTest.updateCustomerProfileImageId("2222", id);

        // Then
        Optional<Customer> customerOptional = underTest.selectCustomerById(id);
        assertThat(customerOptional)
                .isPresent()
                .hasValueSatisfying(
                        c -> assertThat(c.getProfileImageId()).isEqualTo("2222")
                );
    }

    @Test
    void insertCustomerStoresTheUserRoleByDefault() {
        // Given
        String email = FAKER.internet().safeEmailAddress() + "-" + UUID.randomUUID();
        Customer customer = new Customer(
                FAKER.name().fullName(),
                email,
                "password", 20,
                Gender.MALE);

        // When
        underTest.insertCustomer(customer);

        // Then
        assertThat(underTest.selectUserByEmail(email))
                .hasValueSatisfying(c -> assertThat(c.getRole()).isEqualTo(Role.USER));
    }

    @Test
    void insertCustomerStoresAndMapsTheAdminRole() {
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
        underTest.insertCustomer(customer);

        // Then
        Customer actual = underTest.selectUserByEmail(email).orElseThrow();
        assertThat(actual.getRole()).isEqualTo(Role.ADMIN);
        assertThat(underTest.selectCustomerById(actual.getId()))
                .hasValueSatisfying(c -> assertThat(c.getRole()).isEqualTo(Role.ADMIN));
    }
}
