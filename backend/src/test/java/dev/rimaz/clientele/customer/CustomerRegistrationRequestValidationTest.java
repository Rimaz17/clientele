package dev.rimaz.clientele.customer;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerRegistrationRequestValidationTest {

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
    void validRequestHasNoViolations() {
        assertThat(validator.validate(request("Alex", "alex@clientele.dev", "password", 30, Gender.MALE)))
                .isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "SixteenCharsLong"})
    void rejectsBlankOrLongName(String name) {
        assertThat(violatedFields(request(name, "alex@clientele.dev", "password", 30, Gender.MALE)))
                .containsExactly("name");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not-an-email", "alex@@clientele.dev"})
    void rejectsBlankOrInvalidEmail(String email) {
        assertThat(violatedFields(request("Alex", email, "password", 30, Gender.MALE)))
                .containsExactly("email");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "sixteen-chars-xx"})
    void rejectsBlankShortOrLongPassword(String password) {
        assertThat(violatedFields(request("Alex", "alex@clientele.dev", password, 30, Gender.MALE)))
                .containsExactly("password");
    }

    @ParameterizedTest
    @ValueSource(ints = {15, 101})
    void rejectsAgeOutsideRange(int age) {
        assertThat(violatedFields(request("Alex", "alex@clientele.dev", "password", age, Gender.MALE)))
                .containsExactly("age");
    }

    @ParameterizedTest
    @ValueSource(ints = {16, 100})
    void acceptsAgeAtRangeBoundaries(int age) {
        assertThat(validator.validate(request("Alex", "alex@clientele.dev", "password", age, Gender.FEMALE)))
                .isEmpty();
    }

    @Test
    void rejectsMissingAgeAndGender() {
        assertThat(violatedFields(request("Alex", "alex@clientele.dev", "password", null, null)))
                .containsExactlyInAnyOrder("age", "gender");
    }

    private static CustomerRegistrationRequest request(String name, String email, String password,
                                                       Integer age, Gender gender) {
        return new CustomerRegistrationRequest(name, email, password, age, gender);
    }

    private static Set<String> violatedFields(CustomerRegistrationRequest request) {
        Set<ConstraintViolation<CustomerRegistrationRequest>> violations = validator.validate(request);
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
