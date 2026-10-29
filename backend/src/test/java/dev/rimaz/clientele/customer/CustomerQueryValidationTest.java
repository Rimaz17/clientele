package dev.rimaz.clientele.customer;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Sort;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

class CustomerQueryValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void missingValuesFallBackToTheDefaults() {
        CustomerQuery query = new CustomerQuery(null, null, null, null, null, null, null);

        assertThat(query.page()).isZero();
        assertThat(query.size()).isEqualTo(20);
        assertThat(query.sortField()).isEqualTo(CustomerSortField.CREATED_AT);
        assertThat(query.sortDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(violations(query)).isEmpty();
    }

    @Test
    void searchIsTrimmedAndBlankSearchIsIgnored() {
        assertThat(query(" ali ").search()).isEqualTo("ali");
        assertThat(query("   ").search()).isNull();
    }

    @Test
    void validValuesHaveNoViolations() {
        CustomerQuery query = new CustomerQuery(3, 100, "age,desc", "ali", Gender.FEMALE, 16, 100);

        assertThat(violations(query)).isEmpty();
        assertThat(query.sortField()).isEqualTo(CustomerSortField.AGE);
        assertThat(query.sortDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @ParameterizedTest
    @EnumSource(CustomerSortField.class)
    void everySortFieldIsAcceptedInBothDirections(CustomerSortField field) {
        for (String sort : new String[]{field.property(), field.property() + ",asc", field.property() + ",desc"}) {
            CustomerQuery query = new CustomerQuery(null, null, sort, null, null, null, null);

            assertThat(violations(query)).isEmpty();
            assertThat(query.sortField()).isEqualTo(field);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"password", "name,up", "name;DROP TABLE customer", "NAME,asc", "name,asc,age"})
    void rejectsSortOutsideTheWhitelist(String sort) {
        assertThat(violations(new CustomerQuery(null, null, sort, null, null, null, null)))
                .containsOnlyKeys("sort");
    }

    @Test
    void rejectsANegativePage() {
        assertThat(violations(new CustomerQuery(-1, null, null, null, null, null, null)))
                .containsExactly(entry("page", "Page must be 0 or more"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 101})
    void rejectsASizeOutsideOneToOneHundred(int size) {
        assertThat(violations(new CustomerQuery(null, size, null, null, null, null, null)))
                .containsExactly(entry("size", "Size must be between 1 and 100"));
    }

    @Test
    void rejectsASearchLongerThan100Characters() {
        assertThat(violations(query("a".repeat(101)))).containsOnlyKeys("search");
        assertThat(violations(query("a".repeat(100)))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {15, 101})
    void rejectsAgesOutsideTheAllowedRange(int age) {
        assertThat(violations(new CustomerQuery(null, null, null, null, null, age, null)))
                .containsOnlyKeys("minAge");
        assertThat(violations(new CustomerQuery(null, null, null, null, null, null, age)))
                .containsOnlyKeys("maxAge");
    }

    @Test
    void rejectsAMinimumAgeAboveTheMaximumAge() {
        assertThat(violations(new CustomerQuery(null, null, null, null, null, 40, 30)))
                .containsExactly(entry("maxAge", "Maximum age must not be less than minimum age"));
    }

    @Test
    void acceptsEqualMinimumAndMaximumAges() {
        assertThat(violations(new CustomerQuery(null, null, null, null, null, 30, 30))).isEmpty();
    }

    private static CustomerQuery query(String search) {
        return new CustomerQuery(null, null, null, search, null, null, null);
    }

    private static Map<String, String> violations(CustomerQuery query) {
        return validator.validate(query)
                .stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage
                ));
    }
}
