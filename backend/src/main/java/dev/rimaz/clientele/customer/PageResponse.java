package dev.rimaz.clientele.customer;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.function.Function;

/**
 * One page of a list. This is the API's own stable contract rather than
 * Spring Data's {@code Page} JSON, which exposes internal details.
 */
public record PageResponse<T>(
        @Schema(description = "The items on this page; empty past the last page")
        List<T> content,
        @Schema(description = "This page's number, counted from 0", example = "0")
        int page,
        @Schema(description = "The requested page size", example = "20")
        int size,
        @Schema(description = "Matching items on all pages", example = "132")
        long totalElements,
        @Schema(description = "Number of pages; 0 when nothing matches", example = "7")
        int totalPages
) {

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = (int) ((totalElements + size - 1) / size);
        return new PageResponse<>(List.copyOf(content), page, size, totalElements, totalPages);
    }

    public <R> PageResponse<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapped = content.stream().<R>map(mapper).toList();
        return new PageResponse<>(mapped, page, size, totalElements, totalPages);
    }
}
