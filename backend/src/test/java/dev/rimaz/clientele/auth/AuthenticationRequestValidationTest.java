package dev.rimaz.clientele.auth;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationRequestValidationTest {

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
        assertThat(validator.validate(new AuthenticationRequest("alex@clientele.dev", "password"))).isEmpty();
    }

    @Test
    void rejectsBlankUsernameAndPassword() {
        assertThat(violatedFields(new AuthenticationRequest(" ", null)))
                .containsExactlyInAnyOrder("username", "password");
    }

    private static Set<String> violatedFields(AuthenticationRequest request) {
        return validator.validate(request)
                .stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
