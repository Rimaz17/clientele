package dev.rimaz.clientele.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login credentials")
public record AuthenticationRequest(
        @Schema(description = "The customer's email", example = "alex.smith@example.com")
        @NotBlank(message = "Username is required")
        String username,

        @Schema(description = "The customer's password", example = "secret-pw")
        @NotBlank(message = "Password is required")
        String password
) {
}
