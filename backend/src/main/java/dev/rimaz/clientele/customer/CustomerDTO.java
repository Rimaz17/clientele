package dev.rimaz.clientele.customer;

import java.time.Instant;
import java.util.List;

public record CustomerDTO(
        Integer id,
        String name,
        String email,
        Gender gender,
        Integer age,
        List<String> roles,
        String username,
        String profileImageId,
        Instant createdAt
) {

}
