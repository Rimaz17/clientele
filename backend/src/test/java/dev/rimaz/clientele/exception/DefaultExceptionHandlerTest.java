package dev.rimaz.clientele.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.List;

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
    void invalidRequestBodyReturns400WithSortedFieldErrors() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "name", "Name is required"));
        bindingResult.addError(new FieldError("request", "age", "Age must be at least 16"));
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiError> response = underTest.handleException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.statusCode()).isEqualTo(400);
        assertThat(body.message()).isEqualTo("Request validation failed");
        assertThat(body.path()).isEqualTo(PATH);
        assertThat(body.errors()).containsExactly(
                new ApiFieldError("age", "Age must be at least 16"),
                new ApiFieldError("name", "Name is required")
        );
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
    void missingRequestParameterReturns400() {
        ResponseEntity<ApiError> response = underTest.handleException(
                new MissingServletRequestParameterException("file", "MultipartFile"), request);

        assertApiError(response, HttpStatus.BAD_REQUEST, "Required parameter 'file' is missing");
    }

    @Test
    void missingRequestPartReturns400() {
        ResponseEntity<ApiError> response = underTest.handleException(
                new MissingServletRequestPartException("file"), request);

        assertApiError(response, HttpStatus.BAD_REQUEST, "Required part 'file' is missing");
    }

    @Test
    void unsupportedMethodReturns405() {
        ResponseEntity<ApiError> response = underTest.handleException(
                new HttpRequestMethodNotSupportedException("PATCH"), request);

        assertApiError(response, HttpStatus.METHOD_NOT_ALLOWED, "Request method 'PATCH' is not supported");
    }

    @Test
    void unsupportedMediaTypeReturns415() {
        HttpMediaTypeNotSupportedException exception = new HttpMediaTypeNotSupportedException(
                MediaType.TEXT_PLAIN, List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<ApiError> response = underTest.handleException(exception, request);

        assertApiError(response, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content type 'text/plain' is not supported");
    }

    @Test
    void maxUploadSizeExceededReturns413() {
        ResponseEntity<ApiError> response = underTest.handleException(
                new MaxUploadSizeExceededException(10 * 1024 * 1024), request);

        assertApiError(response, HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file is too large");
    }

    @Test
    void insufficientAuthenticationReturns403() {
        ResponseEntity<ApiError> response = underTest.handleException(
                new InsufficientAuthenticationException("Full authentication is required"), request);

        assertApiError(response, HttpStatus.FORBIDDEN, "Full authentication is required");
    }

    @Test
    void accessDeniedReturns403() {
        ResponseEntity<ApiError> response = underTest.handleException(
                new AccessDeniedException("Access is denied"), request);

        assertApiError(response, HttpStatus.FORBIDDEN, "Access denied");
    }

    @Test
    void badCredentialsReturns401() {
        ResponseEntity<ApiError> response = underTest.handleException(
                new BadCredentialsException("Bad credentials"), request);

        assertApiError(response, HttpStatus.UNAUTHORIZED, "Bad credentials");
    }

    @Test
    void unexpectedExceptionReturns500WithoutInternalDetails() {
        ResponseEntity<ApiError> response = underTest.handleException(
                new IllegalStateException("PreparedStatementCallback; bad SQL grammar"), request);

        assertApiError(response, HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
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
        assertThat(body.errors()).isEmpty();
    }
}
