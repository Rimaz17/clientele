package dev.rimaz.clientele.customer;

public record CustomerUpdateRequest(
        String name,
        String email,
        Integer age
) {
}
