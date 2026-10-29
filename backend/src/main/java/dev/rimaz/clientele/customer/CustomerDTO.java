package dev.rimaz.clientele.customer;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "A customer as the API shows it. The password is never included.")
public record CustomerDTO(
        @Schema(example = "1")
        Integer id,
        @Schema(example = "Alex Smith")
        String name,
        @Schema(example = "alex.smith@example.com")
        String email,
        @Schema(example = "FEMALE")
        Gender gender,
        @Schema(example = "30")
        Integer age,
        @ArraySchema(
                arraySchema = @Schema(description = "Authorities: ROLE_USER, or ROLE_ADMIN for admins"),
                schema = @Schema(example = "ROLE_USER", allowableValues = {"ROLE_USER", "ROLE_ADMIN"})
        )
        List<String> roles,
        @Schema(description = "The login name, the same as the email", example = "alex.smith@example.com")
        String username,
        @Schema(description = "Id of the profile picture, or null when there is none",
                example = "1b9f2c04-3e5d-4a8f-9c41-7d2e6b0a5f13", nullable = true)
        String profileImageId,
        @Schema(description = "When the customer signed up, in UTC", example = "2026-03-14T09:26:53.589Z")
        Instant createdAt
) {

}
