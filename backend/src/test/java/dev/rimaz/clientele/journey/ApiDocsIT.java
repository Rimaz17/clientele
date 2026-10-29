package dev.rimaz.clientele.journey;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.hamcrest.Matchers.hasSize;

@IntegrationTest
class ApiDocsIT {

    @Autowired
    private WebTestClient webTestClient;

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
}
