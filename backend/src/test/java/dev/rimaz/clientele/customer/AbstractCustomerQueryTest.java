package dev.rimaz.clientele.customer;

import dev.rimaz.clientele.AbstractTestcontainers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The customer list behaviour every {@link CustomerDao} must have, run against
 * a real PostgreSQL database by one subclass per implementation.
 * <p>
 * The database is shared between the tests of a class, so every test tags its
 * rows with a random marker and searches for it to see only its own rows.
 */
abstract class AbstractCustomerQueryTest extends AbstractTestcontainers {

    private String marker;

    protected abstract CustomerDao dao();

    @BeforeEach
    void createMarker() {
        marker = "m" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    @Test
    void searchMatchesNameOrEmailIgnoringCase() {
        insert("Zed " + marker.toUpperCase(Locale.ROOT), "zed-" + UUID.randomUUID() + "@example.com", 30, Gender.MALE);
        insert("Yan", marker + "@example.com", 30, Gender.MALE);
        insert("Xu", "xu-" + UUID.randomUUID() + "@example.com", 30, Gender.MALE);

        PageResponse<Customer> page = dao().selectCustomers(query(marker, null, null, null, "name,desc"));

        assertThat(names(page)).containsExactly("Zed " + marker.toUpperCase(Locale.ROOT), "Yan");
        assertThat(page.totalElements()).isEqualTo(2);
    }

    @Test
    void wildcardsInTheSearchAreMatchedLiterally() {
        insert(marker + "_50%", uniqueEmail(), 30, Gender.MALE);
        insert(marker + "a50b", uniqueEmail(), 30, Gender.MALE);
        insert(marker + "\\x", uniqueEmail(), 30, Gender.MALE);

        assertThat(names(dao().selectCustomers(query(marker + "_50%", null, null, null, null))))
                .containsExactly(marker + "_50%");
        assertThat(names(dao().selectCustomers(query(marker + "\\", null, null, null, null))))
                .containsExactly(marker + "\\x");
        assertThat(dao().selectCustomers(query(marker + "%", null, null, null, null)).content())
                .isEmpty();
    }

    @Test
    void filtersByGender() {
        insert("Ann", uniqueEmail(), 30, Gender.FEMALE);
        insert("Bob", uniqueEmail(), 30, Gender.MALE);
        insert("Cid", uniqueEmail(), 30, Gender.MALE);

        assertThat(names(dao().selectCustomers(query(marker, Gender.FEMALE, null, null, null))))
                .containsExactly("Ann");
        assertThat(names(dao().selectCustomers(query(marker, Gender.MALE, null, null, null))))
                .containsExactly("Bob", "Cid");
    }

    @Test
    void filtersByAnInclusiveAgeRange() {
        insert("Twenty", uniqueEmail(), 20, Gender.MALE);
        insert("Thirty", uniqueEmail(), 30, Gender.MALE);
        insert("Forty", uniqueEmail(), 40, Gender.MALE);

        assertThat(names(dao().selectCustomers(query(marker, null, 30, null, "age,asc"))))
                .containsExactly("Thirty", "Forty");
        assertThat(names(dao().selectCustomers(query(marker, null, null, 30, "age,asc"))))
                .containsExactly("Twenty", "Thirty");
        assertThat(names(dao().selectCustomers(query(marker, null, 30, 30, "age,asc"))))
                .containsExactly("Thirty");
    }

    @Test
    void combinesAllFilters() {
        insert("Ann", uniqueEmail(), 25, Gender.FEMALE);
        insert("Bea", uniqueEmail(), 45, Gender.FEMALE);
        insert("Cid", uniqueEmail(), 25, Gender.MALE);
        insert("Other", "other-" + UUID.randomUUID() + "@example.com", 25, Gender.FEMALE);

        PageResponse<Customer> page = dao().selectCustomers(query(marker, Gender.FEMALE, 18, 30, null));

        assertThat(names(page)).containsExactly("Ann");
        assertThat(page.totalElements()).isEqualTo(1);
    }

    @ParameterizedTest
    @CsvSource({
            "id,asc,     Bob|Cid|Ann",
            "id,desc,    Ann|Cid|Bob",
            "name,asc,   Ann|Bob|Cid",
            "name,desc,  Cid|Bob|Ann",
            "email,asc,  Cid|Ann|Bob",
            "email,desc, Bob|Ann|Cid",
            "age,asc,    Bob|Ann|Cid",
            "age,desc,   Cid|Ann|Bob"
    })
    void sortsByEveryFieldInBothDirections(String field, String direction, String expectedNames) {
        // Inserted in an order that differs from every sort order below.
        insert("Bob", "c-" + marker + "@example.com", 20, Gender.MALE);
        insert("Cid", "a-" + marker + "@example.com", 40, Gender.MALE);
        insert("Ann", "b-" + marker + "@example.com", 30, Gender.FEMALE);

        PageResponse<Customer> page = dao().selectCustomers(query(marker, null, null, null, field + "," + direction));

        assertThat(names(page)).containsExactly(expectedNames.split("\\|"));
    }

    @Test
    void breaksSortTiesById() {
        insert("Ann", uniqueEmail(), 30, Gender.FEMALE);
        insert("Bob", uniqueEmail(), 30, Gender.MALE);
        insert("Cid", uniqueEmail(), 30, Gender.MALE);

        assertThat(names(dao().selectCustomers(query(marker, null, null, null, "age,desc"))))
                .containsExactly("Ann", "Bob", "Cid");
    }

    @Test
    void returnsPagesWithTotals() {
        for (String name : List.of("A", "B", "C", "D", "E")) {
            insert(name, uniqueEmail(), 30, Gender.MALE);
        }

        PageResponse<Customer> first = dao().selectCustomers(page(0, 2));
        PageResponse<Customer> second = dao().selectCustomers(page(1, 2));
        PageResponse<Customer> last = dao().selectCustomers(page(2, 2));
        PageResponse<Customer> beyond = dao().selectCustomers(page(3, 2));

        assertThat(names(first)).containsExactly("A", "B");
        assertThat(names(second)).containsExactly("C", "D");
        assertThat(names(last)).containsExactly("E");
        assertThat(beyond.content()).isEmpty();
        assertThat(List.of(first, second, last, beyond)).allSatisfy(page -> {
            assertThat(page.totalElements()).isEqualTo(5);
            assertThat(page.totalPages()).isEqualTo(3);
            assertThat(page.size()).isEqualTo(2);
        });
        assertThat(last.page()).isEqualTo(2);
    }

    @Test
    void returnsAnEmptyPageWhenNothingMatches() {
        PageResponse<Customer> page = dao().selectCustomers(query(marker, null, null, null, null));

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        assertThat(page.totalPages()).isZero();
    }

    @Test
    void mapsEveryColumnOfTheListedCustomers() {
        String email = uniqueEmail();
        dao().insertCustomer(new Customer(null, "Ann", email, "password", 30, Gender.FEMALE, null, Role.ADMIN));

        Customer listed = dao().selectCustomers(query(marker, null, null, null, null)).content().get(0);

        assertThat(listed.getId()).isNotNull();
        assertThat(listed.getEmail()).isEqualTo(email);
        assertThat(listed.getPassword()).isEqualTo("password");
        assertThat(listed.getAge()).isEqualTo(30);
        assertThat(listed.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(listed.getRole()).isEqualTo(Role.ADMIN);
    }

    private void insert(String name, String email, int age, Gender gender) {
        dao().insertCustomer(new Customer(name, email, "password", age, gender));
    }

    // Tagged with the marker, so a search for the marker finds it by email.
    private String uniqueEmail() {
        return marker + "-" + UUID.randomUUID() + "@example.com";
    }

    private CustomerQuery query(String search, Gender gender, Integer minAge, Integer maxAge, String sort) {
        return new CustomerQuery(null, null, sort, search, gender, minAge, maxAge);
    }

    private CustomerQuery page(int page, int size) {
        return new CustomerQuery(page, size, "name,asc", marker, null, null, null);
    }

    private static List<String> names(PageResponse<Customer> page) {
        return page.content().stream().map(Customer::getName).toList();
    }
}
