package dev.rimaz.clientele.journey;

import dev.rimaz.clientele.auth.AuthenticationRequest;
import dev.rimaz.clientele.customer.CustomerDTO;
import dev.rimaz.clientele.customer.CustomerRegistrationRequest;
import dev.rimaz.clientele.customer.CustomerUpdateRequest;
import dev.rimaz.clientele.customer.Gender;
import dev.rimaz.clientele.customer.PageResponse;
import dev.rimaz.clientele.exception.ApiError;
import dev.rimaz.clientele.exception.ApiFieldError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@IntegrationTest
class ValidationIT {

    private static final String CUSTOMER_PATH = "/api/v1/customers";
    private static final String LOGIN_PATH = "/api/v1/auth/login";

    @Autowired
    private WebTestClient webTestClient;

    private String email;

    @BeforeEach
    void setUp() {
        email = "validation-" + UUID.randomUUID() + "@clientele.dev";
    }

    @Test
    void validRegistrationSucceeds() {
        webTestClient.post()
                .uri(CUSTOMER_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(registration("Alex", email, "password", 16, Gender.FEMALE)),
                        CustomerRegistrationRequest.class)
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .exists(AUTHORIZATION);
    }

    @Test
    void blankNameIsRejected() {
        assertRegistrationRejected(registration(" ", email, "password", 30, Gender.MALE),
                new ApiFieldError("name", "Name is required"));
    }

    @Test
    void invalidEmailIsRejected() {
        assertRegistrationRejected(registration("Alex", "not-an-email", "password", 30, Gender.MALE),
                new ApiFieldError("email", "Email must be a valid email address"));
    }

    @Test
    void underageCustomerIsRejected() {
        assertRegistrationRejected(registration("Alex", email, "password", 15, Gender.MALE),
                new ApiFieldError("age", "Age must be at least 16"));
    }

    @Test
    void customerOlderThan100IsRejected() {
        assertRegistrationRejected(registration("Alex", email, "password", 101, Gender.MALE),
                new ApiFieldError("age", "Age must be at most 100"));
    }

    @Test
    void missingGenderIsRejected() {
        assertRegistrationRejected(registration("Alex", email, "password", 30, null),
                new ApiFieldError("gender", "Gender is required"));
    }

    @Test
    void shortPasswordIsRejected() {
        assertRegistrationRejected(registration("Alex", email, "abc", 30, Gender.MALE),
                new ApiFieldError("password", "Password must be between 4 and 15 characters"));
    }

    @Test
    void blankLoginFieldsAreRejected() {
        ApiError error = webTestClient.post()
                .uri(LOGIN_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(new AuthenticationRequest("", "")), AuthenticationRequest.class)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ApiError.class)
                .returnResult()
                .getResponseBody();

        assertThat(error).isNotNull();
        assertThat(error.message()).isEqualTo("Request validation failed");
        assertThat(error.errors()).containsExactly(
                new ApiFieldError("password", "Password is required"),
                new ApiFieldError("username", "Username is required")
        );
    }

    @Test
    void invalidUpdateIsRejected() {
        String bearerToken = registerValidCustomer();
        int id = findCustomer(bearerToken).id();

        ApiError error = webTestClient.put()
                .uri(CUSTOMER_PATH + "/{id}", id)
                .header(AUTHORIZATION, bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(new CustomerUpdateRequest("  ", "bad-email", 101)), CustomerUpdateRequest.class)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ApiError.class)
                .returnResult()
                .getResponseBody();

        assertThat(error).isNotNull();
        assertThat(error.errors())
                .extracting(ApiFieldError::field)
                .containsExactly("age", "email", "name");
        assertThat(findCustomer(bearerToken).name()).isEqualTo("Alex");
    }

    @Test
    void emptyProfileImageIsRejected() {
        String bearerToken = registerValidCustomer();
        int id = findCustomer(bearerToken).id();

        ApiError error = uploadProfileImage(bearerToken, id, new byte[0], MediaType.IMAGE_JPEG);

        assertThat(error.message()).isEqualTo("profile image file is empty");
        assertThat(findCustomer(bearerToken).profileImageId()).isNull();
    }

    @Test
    void nonImageProfileImageIsRejected() {
        String bearerToken = registerValidCustomer();
        int id = findCustomer(bearerToken).id();

        ApiError error = uploadProfileImage(bearerToken, id, "not an image".getBytes(), MediaType.TEXT_PLAIN);

        assertThat(error.message()).isEqualTo("profile image must be a JPEG, PNG or WebP file");
        assertThat(findCustomer(bearerToken).profileImageId()).isNull();
    }

    private void assertRegistrationRejected(CustomerRegistrationRequest request,
                                            ApiFieldError expectedError) {
        ApiError error = webTestClient.post()
                .uri(CUSTOMER_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(request), CustomerRegistrationRequest.class)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ApiError.class)
                .returnResult()
                .getResponseBody();

        assertThat(error).isNotNull();
        assertThat(error.statusCode()).isEqualTo(400);
        assertThat(error.path()).isEqualTo(CUSTOMER_PATH);
        assertThat(error.message()).isEqualTo("Request validation failed");
        assertThat(error.errors()).containsExactly(expectedError);

        // Nothing was stored: the rejected customer cannot log in.
        webTestClient.post()
                .uri(LOGIN_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(new AuthenticationRequest(request.email(), request.password())),
                        AuthenticationRequest.class)
                .exchange()
                .expectStatus()
                .is4xxClientError();
    }

    private ApiError uploadProfileImage(String bearerToken, int customerId,
                                        byte[] content, MediaType contentType) {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return "upload";
            }
        }).contentType(contentType);

        return webTestClient.post()
                .uri(CUSTOMER_PATH + "/{id}/profile-image", customerId)
                .header(AUTHORIZATION, bearerToken)
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ApiError.class)
                .returnResult()
                .getResponseBody();
    }

    private String registerValidCustomer() {
        String jwtToken = webTestClient.post()
                .uri(CUSTOMER_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(registration("Alex", email, "password", 30, Gender.MALE)),
                        CustomerRegistrationRequest.class)
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult(Void.class)
                .getResponseHeaders()
                .getFirst(AUTHORIZATION);
        return "Bearer " + jwtToken;
    }

    private CustomerDTO findCustomer(String bearerToken) {
        return webTestClient.get()
                .uri(CUSTOMER_PATH + "?search={email}", email)
                .header(AUTHORIZATION, bearerToken)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageResponse<CustomerDTO>>() {
                })
                .returnResult()
                .getResponseBody()
                .content()
                .stream()
                .filter(customer -> customer.email().equals(email))
                .findFirst()
                .orElseThrow();
    }

    private static CustomerRegistrationRequest registration(String name, String email, String password,
                                                            Integer age, Gender gender) {
        return new CustomerRegistrationRequest(name, email, password, age, gender);
    }
}
