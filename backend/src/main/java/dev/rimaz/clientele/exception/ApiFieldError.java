package dev.rimaz.clientele.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A field that failed validation")
public record ApiFieldError(
        @Schema(description = "The field or query parameter", example = "email")
        String field,
        @Schema(description = "Why it is invalid", example = "Email must be a valid email address")
        String message
) {
}
