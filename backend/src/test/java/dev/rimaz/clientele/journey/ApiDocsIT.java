package dev.rimaz.clientele.journey;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;

@IntegrationTest
class ApiDocsIT {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ObjectMapper objectMapper;

    // Every status an endpoint can return is documented, and no others.
    // Errors use the ApiError schema.
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "get    | /api/v1/customers                              | 200 400 403",
            "post   | /api/v1/customers                              | 200 400 409",
            "get    | /api/v1/customers/{customerId}                 | 200 400 403 404",
            "put    | /api/v1/customers/{customerId}                 | 200 400 403 404 409",
            "delete | /api/v1/customers/{customerId}                 | 200 400 403 404",
            "post   | /api/v1/customers/{customerId}/profile-image   | 200 400 403 404 413",
            "get    | /api/v1/customers/{customerId}/profile-image   | 200 400 404"
    })
    void documentsEveryResponseOfAnEndpoint(String method, String path, String statuses) throws Exception {
        JsonNode operation = apiDocs().path("paths").path(path).path(method);
        assertThat(operation.isMissingNode()).as("%s %s is documented", method, path).isFalse();
        assertThat(operation.path("summary").asText()).isNotBlank();
        assertThat(operation.path("tags").isArray()).isTrue();

        JsonNode responses = operation.path("responses");
        List<String> documented = new ArrayList<>();
        responses.fieldNames().forEachRemaining(documented::add);
        assertThat(documented).containsExactlyInAnyOrder(statuses.split(" "));
        documented.stream()
                .filter(status -> !status.startsWith("2"))
                .forEach(status -> assertThat(responses.path(status)
                        .at("/content/application~1json/schema/$ref").asText())
                        .as("%s %s %s", method, path, status)
                        .isEqualTo("#/components/schemas/ApiError"));
    }

    @Test
    void servesTheOpenApiDocumentWithoutAToken() {
        webTestClient.get()
                .uri("/v3/api-docs")
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.openapi").exists();
    }

    @Test
    void describesTheApiAndItsBearerToken() {
        webTestClient.get()
                .uri("/v3/api-docs")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.info.title").isEqualTo("Clientele API")
                .jsonPath("$.info.version").isEqualTo("v1")
                .jsonPath("$.components.securitySchemes.bearerAuth.type").isEqualTo("http")
                .jsonPath("$.components.securitySchemes.bearerAuth.scheme").isEqualTo("bearer")
                .jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").isEqualTo("JWT")
                .jsonPath("$.security[0].bearerAuth").isArray();
    }

    @Test
    void documentsEveryQueryParameterOfTheCustomerList() throws Exception {
        JsonNode parameters = apiDocs().at("/paths/~1api~1v1~1customers/get/parameters");

        List<String> names = new ArrayList<>();
        parameters.forEach(parameter -> {
            names.add(parameter.path("name").asText());
            assertThat(parameter.path("in").asText()).isEqualTo("query");
            assertThat(parameter.path("required").asBoolean()).isFalse();
            assertThat(parameter.path("description").asText()).isNotBlank();
        });
        assertThat(names).containsExactly("page", "size", "sort", "search", "gender", "minAge", "maxAge");
        assertThat(parameter(parameters, "size").at("/schema/maximum").asInt()).isEqualTo(100);
        assertThat(parameter(parameters, "sort").at("/schema/default").asText()).isEqualTo("createdAt,desc");
        assertThat(parameter(parameters, "gender").at("/schema/enum").toString()).isEqualTo("[\"MALE\",\"FEMALE\"]");
    }

    @Test
    void marksThePublicEndpointsAsNotNeedingAToken() {
        webTestClient.get()
                .uri("/v3/api-docs")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.paths['/api/v1/auth/login'].post.security").value(hasSize(0))
                .jsonPath("$.paths['/api/v1/customers'].post.security").value(hasSize(0))
                .jsonPath("$.paths['/api/v1/customers/{customerId}/profile-image'].get.security").value(hasSize(0))
                .jsonPath("$.paths['/ping'].get.security").value(hasSize(0))
                .jsonPath("$.paths['/actuator/health'].get.security").value(hasSize(0))
                .jsonPath("$.paths['/actuator/info'].get.security").value(hasSize(0));
    }

    @Test
    void leavesProtectedEndpointsOnTheDefaultBearerRequirement() {
        webTestClient.get()
                .uri("/v3/api-docs")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.paths['/api/v1/customers'].get.security").doesNotExist()
                .jsonPath("$.paths['/api/v1/customers/stats'].get.security").doesNotExist()
                .jsonPath("$.paths['/api/v1/customers/{customerId}/profile-image'].post.security").doesNotExist();
    }

    @Test
    void documentsThePublicActuatorEndpoints() {
        webTestClient.get()
                .uri("/v3/api-docs")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.paths['/actuator/health'].get").exists()
                .jsonPath("$.paths['/actuator/info'].get").exists();
    }

    @Test
    void doesNotAddEveryExceptionHandlerResponseToEveryOperation() {
        // Without springdoc.override-with-generic-response=false, /ping would
        // claim to return 404, 409 and every other handled error.
        webTestClient.get()
                .uri("/v3/api-docs")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.paths['/ping'].get.responses['200']").exists()
                .jsonPath("$.paths['/ping'].get.responses['409']").doesNotExist()
                .jsonPath("$.paths['/ping'].get.responses['500']").doesNotExist();
    }

    @Test
    void servesSwaggerUiWithoutAToken() {
        webTestClient.get()
                .uri("/swagger-ui/index.html")
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.TEXT_HTML);
    }

    @Test
    void redirectsTheShortSwaggerUiPath() {
        webTestClient.get()
                .uri("/swagger-ui.html")
                .exchange()
                .expectStatus()
                .is3xxRedirection()
                .expectHeader()
                .valueMatches("Location", ".*/swagger-ui/index.html.*");
    }

    private static JsonNode parameter(JsonNode parameters, String name) {
        for (JsonNode parameter : parameters) {
            if (parameter.path("name").asText().equals(name)) {
                return parameter;
            }
        }
        throw new AssertionError("No parameter " + name);
    }

    private JsonNode apiDocs() throws Exception {
        byte[] body = webTestClient.get()
                .uri("/v3/api-docs")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .returnResult()
                .getResponseBody();
        return objectMapper.readTree(body);
    }
}
