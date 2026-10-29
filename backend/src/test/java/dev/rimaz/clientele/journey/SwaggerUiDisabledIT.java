package dev.rimaz.clientele.journey;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * With {@code springdoc.swagger-ui.enabled=false} the UI is gone, but the
 * OpenAPI document is still served.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {
        "clientele.seed.enabled=false",
        "springdoc.swagger-ui.enabled=false"
})
class SwaggerUiDisabledIT {

    @Autowired
    private WebTestClient webTestClient;

    // Without a token a missing page answers 403 rather than 404, because
    // Spring forwards it to /error, which is not public.
    @Test
    void swaggerUiIsNotServed() {
        webTestClient.get()
                .uri("/swagger-ui/index.html")
                .exchange()
                .expectStatus()
                .is4xxClientError();
    }

    @Test
    void theOpenApiDocumentIsStillServed() {
        webTestClient.get()
                .uri("/v3/api-docs")
                .exchange()
                .expectStatus()
                .isOk();
    }
}
