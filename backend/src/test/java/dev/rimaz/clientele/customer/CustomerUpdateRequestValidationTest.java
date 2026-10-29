package dev.rimaz.clientele.customer;

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

class CustomerUpdateRequestValidationTest {

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
    void allFieldsAreOptional() {
        assertThat(validator.validate(new CustomerUpdateRequest(null, null, null))).isEmpty();
    }

    @Test
    void validValuesHaveNoViolations() {
        assertThat(validator.validate(new CustomerUpdateRequest("Alex", "alex@clientele.dev", 30))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "SixteenCharsLong"})
    void rejectsBlankOrLongName(String name) {
        assertThat(violatedFields(new CustomerUpdateRequest(name, null, null))).containsExactly("name");
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-an-email", "alex@@clientele.dev"})
    void rejectsInvalidEmail(String email) {
        assertThat(violatedFields(new CustomerUpdateRequest(null, email, null))).containsExactly("email");
    }

    @ParameterizedTest
    @ValueSource(ints = {15, 101})
    void rejectsAgeOutsideRange(int age) {
        assertThat(violatedFields(new CustomerUpdateRequest(null, null, age))).containsExactly("age");
    }

    private static Set<String> violatedFields(CustomerUpdateRequest request) {
        return validator.validate(request)
                .stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
