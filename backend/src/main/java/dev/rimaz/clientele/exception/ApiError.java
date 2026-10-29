package dev.rimaz.clientele.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "The body of every error response")
public record ApiError(
        @Schema(description = "The request path", example = "/api/v1/customers")
        String path,
        @Schema(description = "What went wrong, safe to show to users", example = "Request validation failed")
        String message,
        @Schema(description = "The HTTP status code", example = "400")
        int statusCode,
        @Schema(description = "When the error happened, in the server's local time", example = "2026-10-30T10:15:30.123")
        LocalDateTime localDateTime,
        @Schema(description = "One entry per invalid field when validation failed; otherwise empty")
        List<ApiFieldError> errors
) {
}
