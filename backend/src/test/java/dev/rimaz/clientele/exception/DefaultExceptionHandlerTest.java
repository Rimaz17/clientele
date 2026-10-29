package dev.rimaz.clientele.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultExceptionHandlerTest {

    private static final String PATH = "/api/v1/customers/1";

    private DefaultExceptionHandler underTest;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        underTest = new DefaultExceptionHandler();
        request = new MockHttpServletRequest("GET", PATH);
    }

    @Test
    void resourceNotFoundReturns404() {
        ResponseEntity<ApiError> response = underTest.handleException(
                new ResourceNotFoundException("customer with id [1] not found"), request);

        assertApiError(response, HttpStatus.NOT_FOUND, "customer with id [1] not found");
    }

    @Test
    void insufficientAuthenticationReturns403() {
        ResponseEntity<ApiError> response = underTest.handleException(
                new InsufficientAuthenticationException("Full authentication is required"), request);

        assertApiError(response, HttpStatus.FORBIDDEN, "Full authentication is required");
    }

    @Test
    void badCredentialsReturns401() {
        ResponseEntity<ApiError> response = underTest.handleException(
                new BadCredentialsException("Bad credentials"), request);

        assertApiError(response, HttpStatus.UNAUTHORIZED, "Bad credentials");
    }

    private void assertApiError(ResponseEntity<ApiError> response,
                                HttpStatus expectedStatus,
                                String expectedMessage) {
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.statusCode()).isEqualTo(expectedStatus.value());
        assertThat(body.message()).isEqualTo(expectedMessage);
        assertThat(body.path()).isEqualTo(PATH);
        assertThat(body.localDateTime()).isNotNull();
    }
}
