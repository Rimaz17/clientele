package dev.rimaz.clientele.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The OpenAPI description served at {@code /v3/api-docs} and shown by Swagger UI.
 * <p>
 * Every operation needs the bearer token unless it says otherwise, so an
 * endpoint that forgets to declare itself public is documented as protected.
 */
@Configuration
public class OpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI clienteleOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Clientele API")
                        .version("v1")
                        .description("""
                                Customer management API: sign up, log in, then list, search, \
                                view, update and delete customers, upload profile pictures and \
                                read dashboard statistics.

                                To call a protected endpoint here, log in with \
                                `POST /api/v1/auth/login`, copy the `token` from the response, \
                                choose **Authorize** and paste the token (without `Bearer`)."""))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("The token returned by login or sign-up.")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
