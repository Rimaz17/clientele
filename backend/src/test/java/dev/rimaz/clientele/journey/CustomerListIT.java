package dev.rimaz.clientele.journey;

import dev.rimaz.clientele.customer.CustomerDTO;
import dev.rimaz.clientele.customer.CustomerRegistrationRequest;
import dev.rimaz.clientele.customer.Gender;
import dev.rimaz.clientele.customer.PageResponse;
import dev.rimaz.clientele.exception.ApiError;
import dev.rimaz.clientele.exception.ApiFieldError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@IntegrationTest
class CustomerListIT {

    private static final String CUSTOMER_PATH = "/api/v1/customers";

    @Autowired
    private WebTestClient webTestClient;

    // The integration tests share one database, so every test tags its
    // customers' emails with a marker and searches for it.
    private String marker;
    private String bearerToken;

    @BeforeEach
    void setUp() {
        marker = "list" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        bearerToken = register("Ann", 25, Gender.FEMALE);
        register("Bob", 35, Gender.MALE);
        register("Cid", 45, Gender.MALE);
    }

    @Test
    void searchesSortsAndPagesTogether() {
        PageResponse<CustomerDTO> first = getPage("?search={search}&sort=age,desc&size=2", marker);
        PageResponse<CustomerDTO> second = getPage("?search={search}&sort=age,desc&size=2&page=1", marker);

        assertThat(names(first)).containsExactly("Cid", "Bob");
        assertThat(names(second)).containsExactly("Ann");
        assertThat(first.totalElements()).isEqualTo(3);
        assertThat(first.totalPages()).isEqualTo(2);
        assertThat(second.page()).isEqualTo(1);
        assertThat(second.size()).isEqualTo(2);
    }

    @Test
    void filtersByGenderAndAge() {
        assertThat(names(getPage("?search={search}&gender=MALE&sort=name", marker)))
                .containsExactly("Bob", "Cid");
        assertThat(names(getPage("?search={search}&minAge=30&maxAge=40", marker)))
                .containsExactly("Bob");
    }

    @Test
    void searchIsCaseInsensitive() {
        assertThat(getPage("?search={search}", marker.toUpperCase()).totalElements()).isEqualTo(3);
    }

    @Test
    void usesTheDefaultsWithoutParameters() {
        PageResponse<CustomerDTO> page = getPage("");

        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(20);
        assertThat(page.content()).hasSizeLessThanOrEqualTo(20);
        assertThat(page.totalElements()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void listsTheNewestSignUpsFirstByDefault() {
        assertThat(names(getPage("?search={search}", marker))).containsExactly("Cid", "Bob", "Ann");
        assertThat(names(getPage("?search={search}&sort=createdAt,asc", marker)))
                .containsExactly("Ann", "Bob", "Cid");
    }

    @Test
    void neverReturnsMoreThanTheRequestedSize() {
        assertThat(getPage("?size=1").content()).hasSize(1);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "size=0              | size    | Size must be between 1 and 100",
            "size=101            | size    | Size must be between 1 and 100",
            "page=-1             | page    | Page must be 0 or more",
            "page=abc            | page    | Invalid value",
            "sort=password       | sort    | Sort must be id, name, email, age or createdAt, optionally followed by ,asc or ,desc",
            "sort=name,up        | sort    | Sort must be id, name, email, age or createdAt, optionally followed by ,asc or ,desc",
            "gender=OTHER        | gender  | Invalid value",
            "minAge=15           | minAge  | Minimum age must be between 16 and 100",
            "maxAge=abc          | maxAge  | Invalid value",
            "minAge=40&maxAge=30 | maxAge  | Maximum age must not be less than minimum age"
    })
    void rejectsInvalidParameters(String parameters, String field, String message) {
        ApiError error = getError("?" + parameters);

        assertThat(error.message()).isEqualTo("Request validation failed");
        assertThat(error.errors()).containsExactly(new ApiFieldError(field, message));
    }

    @Test
    void rejectsASearchLongerThan100Characters() {
        ApiError error = getError("?search=" + "a".repeat(101));

        assertThat(error.errors()).containsExactly(
                new ApiFieldError("search", "Search must be at most 100 characters"));
    }

    @Test
    void requiresAuthentication() {
        webTestClient.get()
                .uri(CUSTOMER_PATH)
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    private String register(String name, int age, Gender gender) {
        String email = name.toLowerCase() + "-" + marker + "-" + UUID.randomUUID() + "@clientele.dev";
        String token = webTestClient.post()
                .uri(CUSTOMER_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(new CustomerRegistrationRequest(name, email, "password", age, gender)),
                        CustomerRegistrationRequest.class)
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult(Void.class)
                .getResponseHeaders()
                .getFirst(AUTHORIZATION);
        return "Bearer " + token;
    }

    private PageResponse<CustomerDTO> getPage(String query, Object... uriVariables) {
        return webTestClient.get()
                .uri(CUSTOMER_PATH + query, uriVariables)
                .header(AUTHORIZATION, bearerToken)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageResponse<CustomerDTO>>() {
                })
                .returnResult()
                .getResponseBody();
    }

    private ApiError getError(String query) {
        ApiError error = webTestClient.get()
                .uri(CUSTOMER_PATH + query)
                .header(AUTHORIZATION, bearerToken)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(ApiError.class)
                .returnResult()
                .getResponseBody();
        assertThat(error).isNotNull();
        assertThat(error.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(error.path()).isEqualTo(CUSTOMER_PATH);
        return error;
    }

    private static List<String> names(PageResponse<CustomerDTO> page) {
        return page.content().stream().map(CustomerDTO::name).toList();
    }
}
