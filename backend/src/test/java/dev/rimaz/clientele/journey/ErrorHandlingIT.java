package dev.rimaz.clientele.journey;

import dev.rimaz.clientele.customer.CustomerDTO;
import dev.rimaz.clientele.customer.CustomerRegistrationRequest;
import dev.rimaz.clientele.customer.CustomerUpdateRequest;
import dev.rimaz.clientele.customer.Gender;
import dev.rimaz.clientele.exception.ApiError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@SpringBootTest(webEnvironment = RANDOM_PORT)
class ErrorHandlingIT {

    private static final String CUSTOMER_PATH = "/api/v1/customers";

    @Autowired
    private WebTestClient webTestClient;

    private CustomerRegistrationRequest registration;
    private String bearerToken;

    @BeforeEach
    void setUp() {
        String email = "error-" + UUID.randomUUID() + "@clientele.dev";
        registration = new CustomerRegistrationRequest("Alex", email, "password", 30, Gender.MALE);
        String jwtToken = webTestClient.post()
                .uri(CUSTOMER_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(registration), CustomerRegistrationRequest.class)
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult(Void.class)
                .getResponseHeaders()
                .getFirst(AUTHORIZATION);
        bearerToken = "Bearer " + jwtToken;
    }

    @Test
    void registeringWithAnExistingEmailReturns409() {
        ApiError error = webTestClient.post()
                .uri(CUSTOMER_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(registration), CustomerRegistrationRequest.class)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.CONFLICT)
                .expectBody(ApiError.class)
                .returnResult()
                .getResponseBody();

        assertApiError(error, HttpStatus.CONFLICT, CUSTOMER_PATH, "email already taken");
    }

    @Test
    void updateWithoutChangesReturns400() {
        int id = findCustomerId(registration.email());
        CustomerUpdateRequest sameValues = new CustomerUpdateRequest(
                registration.name(), registration.email(), registration.age());

        ApiError error = webTestClient.put()
                .uri(CUSTOMER_PATH + "/{id}", id)
                .header(AUTHORIZATION, bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(sameValues), CustomerUpdateRequest.class)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ApiError.class)
                .returnResult()
                .getResponseBody();

        assertApiError(error, HttpStatus.BAD_REQUEST, CUSTOMER_PATH + "/" + id, "no data changes found");
    }

    @Test
    void unknownCustomerReturns404() {
        ApiError error = webTestClient.get()
                .uri(CUSTOMER_PATH + "/{id}", 999999)
                .header(AUTHORIZATION, bearerToken)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ApiError.class)
                .returnResult()
                .getResponseBody();

        assertApiError(error, HttpStatus.NOT_FOUND, CUSTOMER_PATH + "/999999",
                "customer with id [999999] not found");
    }

    @Test
    void nonNumericCustomerIdReturns400() {
        ApiError error = webTestClient.get()
                .uri(CUSTOMER_PATH + "/abc")
                .header(AUTHORIZATION, bearerToken)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ApiError.class)
                .returnResult()
                .getResponseBody();

        assertApiError(error, HttpStatus.BAD_REQUEST, CUSTOMER_PATH + "/abc",
                "Parameter 'customerId' has an invalid value");
    }

    @Test
    void malformedJsonReturns400() {
        ApiError error = webTestClient.post()
                .uri(CUSTOMER_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"name\": \"Alex\", \"email\": ")
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ApiError.class)
                .returnResult()
                .getResponseBody();

        assertApiError(error, HttpStatus.BAD_REQUEST, CUSTOMER_PATH, "Malformed or unreadable request body");
    }

    @Test
    void invalidTokenReturns403() {
        ApiError error = webTestClient.get()
                .uri(CUSTOMER_PATH)
                .header(AUTHORIZATION, "Bearer not-a-real-token")
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody(ApiError.class)
                .returnResult()
                .getResponseBody();

        assertThat(error).isNotNull();
        assertThat(error.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(error.path()).isEqualTo(CUSTOMER_PATH);
    }

    private int findCustomerId(String email) {
        return webTestClient.get()
                .uri(CUSTOMER_PATH)
                .header(AUTHORIZATION, bearerToken)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBodyList(CustomerDTO.class)
                .returnResult()
                .getResponseBody()
                .stream()
                .filter(customer -> customer.email().equals(email))
                .map(CustomerDTO::id)
                .findFirst()
                .orElseThrow();
    }

    private static void assertApiError(ApiError error,
                                       HttpStatus expectedStatus,
                                       String expectedPath,
                                       String expectedMessage) {
        assertThat(error).isNotNull();
        assertThat(error.statusCode()).isEqualTo(expectedStatus.value());
        assertThat(error.path()).isEqualTo(expectedPath);
        assertThat(error.message()).isEqualTo(expectedMessage);
        assertThat(error.localDateTime()).isNotNull();
    }
}
