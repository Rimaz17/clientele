package dev.rimaz.clientele.exception;

public record ApiFieldError(
        String field,
        String message
) {
}
