package dev.rimaz.clientele.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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
    void duplicateResourceReturns409() {
        ResponseEntity<ApiError> response = underTest.handleException(
                new DuplicateResourceException("email already taken"), request);

        assertApiError(response, HttpStatus.CONFLICT, "email already taken");
    }

    @Test
    void requestValidationReturns400() {
        ResponseEntity<ApiError> response = underTest.handleException(
                new RequestValidationException("no data changes found"), request);

        assertApiError(response, HttpStatus.BAD_REQUEST, "no data changes found");
    }

    @Test
    void unreadableBodyReturns400WithoutParserDetails() {
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "JSON parse error: Unexpected character ('}' (code 125))",
                new MockHttpInputMessage(new byte[0]));

        ResponseEntity<ApiError> response = underTest.handleException(exception, request);

        assertApiError(response, HttpStatus.BAD_REQUEST, "Malformed or unreadable request body");
    }

    @Test
    void argumentTypeMismatchReturns400() {
        MethodArgumentTypeMismatchException exception = new MethodArgumentTypeMismatchException(
                "abc", Integer.class, "customerId", null, new NumberFormatException());

        ResponseEntity<ApiError> response = underTest.handleException(exception, request);

        assertApiError(response, HttpStatus.BAD_REQUEST, "Parameter 'customerId' has an invalid value");
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
