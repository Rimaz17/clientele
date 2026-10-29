package dev.rimaz.clientele.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ApiError(
        String path,
        String message,
        int statusCode,
        LocalDateTime localDateTime,
        List<ApiFieldError> errors
) {
}
