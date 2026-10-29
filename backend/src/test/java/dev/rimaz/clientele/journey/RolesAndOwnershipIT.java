package dev.rimaz.clientele.journey;

import dev.rimaz.clientele.auth.AuthenticationRequest;
import dev.rimaz.clientele.auth.AuthenticationResponse;
import dev.rimaz.clientele.customer.CustomerDTO;
import dev.rimaz.clientele.customer.CustomerRegistrationRequest;
import dev.rimaz.clientele.customer.CustomerUpdateRequest;
import dev.rimaz.clientele.customer.Gender;
import dev.rimaz.clientele.exception.ApiError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@IntegrationTest
class RolesAndOwnershipIT {

    private static final String CUSTOMER_PATH = "/api/v1/customers";
    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String PASSWORD = "password";

    @Autowired
    private WebTestClient webTestClient;

    // The dev profile, which the tests run in, bootstraps this admin on startup.
    @Value("${clientele.bootstrap.admin.email}")
    private String adminEmail;
    @Value("${clientele.bootstrap.admin.password}")
    private String adminPassword;

    private TestCustomer alice;
    private TestCustomer bob;

    @BeforeEach
    void setUp() {
        alice = register("Alice");
        bob = register("Bob");
    }

    @Test
    void usersCannotUpdateSomeoneElse() {
        ApiError error = webTestClient.put()
                .uri(CUSTOMER_PATH + "/{id}", bob.id())
                .header(AUTHORIZATION, alice.bearerToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(new CustomerUpdateRequest("Hacked", null, null)), CustomerUpdateRequest.class)
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody(ApiError.class)
                .returnResult()
                .getResponseBody();

        assertAccessDenied(error, CUSTOMER_PATH + "/" + bob.id());
        assertThat(getCustomer(bob.id(), bob.bearerToken()).name()).isEqualTo("Bob");
    }

    @Test
    void usersCannotDeleteSomeoneElse() {
        ApiError error = webTestClient.delete()
                .uri(CUSTOMER_PATH + "/{id}", bob.id())
                .header(AUTHORIZATION, alice.bearerToken())
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody(ApiError.class)
                .returnResult()
                .getResponseBody();

        assertAccessDenied(error, CUSTOMER_PATH + "/" + bob.id());
        assertThat(getCustomer(bob.id(), alice.bearerToken()).id()).isEqualTo(bob.id());
    }

    @Test
    void usersCannotUploadAProfileImageForSomeoneElse() {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        bodyBuilder.part("file", new ClassPathResource("female.jpeg"));

        ApiError error = webTestClient.post()
                .uri(CUSTOMER_PATH + "/{id}/profile-image", bob.id())
                .header(AUTHORIZATION, alice.bearerToken())
                .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody(ApiError.class)
                .returnResult()
                .getResponseBody();

        assertAccessDenied(error, CUSTOMER_PATH + "/" + bob.id() + "/profile-image");
        assertThat(getCustomer(bob.id(), bob.bearerToken()).profileImageId()).isNull();
    }

    @Test
    void usersCanUpdateThemselves() {
        updateCustomer(alice.id(), alice.bearerToken(), new CustomerUpdateRequest("Alicia", null, 41))
                .expectStatus()
                .isOk();

        CustomerDTO updated = getCustomer(alice.id(), alice.bearerToken());
        assertThat(updated.name()).isEqualTo("Alicia");
        assertThat(updated.age()).isEqualTo(41);
    }

    @Test
    void adminsCanUpdateAnyone() {
        String adminToken = login(adminEmail, adminPassword).bearerToken();

        updateCustomer(bob.id(), adminToken, new CustomerUpdateRequest("Robert", null, null))
                .expectStatus()
                .isOk()
                // Only users who change their own email get a new token.
                .expectHeader()
                .doesNotExist(AUTHORIZATION);

        assertThat(getCustomer(bob.id(), bob.bearerToken()).name()).isEqualTo("Robert");
    }

    @Test
    void adminsCanDeleteAnyone() {
        String adminToken = login(adminEmail, adminPassword).bearerToken();

        webTestClient.delete()
                .uri(CUSTOMER_PATH + "/{id}", bob.id())
                .header(AUTHORIZATION, adminToken)
                .exchange()
                .expectStatus()
                .isOk();

        webTestClient.get()
                .uri(CUSTOMER_PATH + "/{id}", bob.id())
                .header(AUTHORIZATION, adminToken)
                .exchange()
                .expectStatus()
                .isNotFound();
    }

    @Test
    void adminsHaveTheAdminRole() {
        AuthenticationResponse admin = login(adminEmail, adminPassword).response();

        assertThat(admin.customerDTO().roles()).containsExactly("ROLE_ADMIN");
    }

    @Test
    void registrationIgnoresARoleField() {
        String email = uniqueEmail();
        String body = """
                {"name": "Mallory", "email": "%s", "password": "%s", "age": 30,
                 "gender": "FEMALE", "role": "ADMIN", "roles": ["ROLE_ADMIN"]}
                """.formatted(email, PASSWORD);

        webTestClient.post()
                .uri(CUSTOMER_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk();

        assertThat(login(email, PASSWORD).response().customerDTO().roles()).containsExactly("ROLE_USER");
    }

    @Test
    void tokensOfDeletedUsersAreRejected() {
        webTestClient.delete()
                .uri(CUSTOMER_PATH + "/{id}", alice.id())
                .header(AUTHORIZATION, alice.bearerToken())
                .exchange()
                .expectStatus()
                .isOk();

        ApiError error = webTestClient.get()
                .uri(CUSTOMER_PATH + "/{id}", bob.id())
                .header(AUTHORIZATION, alice.bearerToken())
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody(ApiError.class)
                .returnResult()
                .getResponseBody();

        assertThat(error).isNotNull();
        assertThat(error.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    private TestCustomer register(String name) {
        String email = uniqueEmail();
        webTestClient.post()
                .uri(CUSTOMER_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(new CustomerRegistrationRequest(name, email, PASSWORD, 30, Gender.FEMALE)),
                        CustomerRegistrationRequest.class)
                .exchange()
                .expectStatus()
                .isOk();
        LoggedIn loggedIn = login(email, PASSWORD);
        return new TestCustomer(loggedIn.response().customerDTO().id(), loggedIn.bearerToken());
    }

    private LoggedIn login(String email, String password) {
        var result = webTestClient.post()
                .uri(LOGIN_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(new AuthenticationRequest(email, password)), AuthenticationRequest.class)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AuthenticationResponse.class)
                .returnResult();
        String token = result.getResponseHeaders().getFirst(AUTHORIZATION);
        return new LoggedIn(result.getResponseBody(), "Bearer " + token);
    }

    private WebTestClient.ResponseSpec updateCustomer(int id, String bearerToken, CustomerUpdateRequest request) {
        return webTestClient.put()
                .uri(CUSTOMER_PATH + "/{id}", id)
                .header(AUTHORIZATION, bearerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(request), CustomerUpdateRequest.class)
                .exchange();
    }

    private CustomerDTO getCustomer(int id, String bearerToken) {
        return webTestClient.get()
                .uri(CUSTOMER_PATH + "/{id}", id)
                .header(AUTHORIZATION, bearerToken)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(CustomerDTO.class)
                .returnResult()
                .getResponseBody();
    }

    private static void assertAccessDenied(ApiError error, String expectedPath) {
        assertThat(error).isNotNull();
        assertThat(error.statusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(error.message()).isEqualTo("Access denied");
        assertThat(error.path()).isEqualTo(expectedPath);
    }

    private static String uniqueEmail() {
        return "roles-" + UUID.randomUUID() + "@clientele.dev";
    }

    private record TestCustomer(int id, String bearerToken) {
    }

    private record LoggedIn(AuthenticationResponse response, String bearerToken) {
    }
}
