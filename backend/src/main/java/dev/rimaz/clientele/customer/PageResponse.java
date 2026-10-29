package dev.rimaz.clientele.customer;

import java.util.List;
import java.util.function.Function;

/**
 * One page of a list. This is the API's own stable contract rather than
 * Spring Data's {@code Page} JSON, which exposes internal details.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
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
