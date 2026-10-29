package dev.rimaz.clientele.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// The rules mirror the React sign-up form so both layers agree.
@Schema(description = "A new customer. Sign-up always creates a regular user.")
public record CustomerRegistrationRequest(
        @Schema(description = "Display name", example = "Alex Smith")
        @NotBlank(message = "Name is required")
        @Size(max = 15, message = "Name must be at most 15 characters")
        String name,

        @Schema(description = "Email, also the login name; must not be taken", example = "alex.smith@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 254, message = "Email must be at most 254 characters")
        String email,

        @Schema(description = "Password, stored only as a BCrypt hash", example = "secret-pw")
        @NotBlank(message = "Password is required")
        @Size(min = 4, max = 15, message = "Password must be between 4 and 15 characters")
        String password,

        @Schema(description = "Age in years", example = "30")
        @NotNull(message = "Age is required")
        @Min(value = 16, message = "Age must be at least 16")
        @Max(value = 100, message = "Age must be at most 100")
        Integer age,

        @Schema(example = "FEMALE")
        @NotNull(message = "Gender is required")
        Gender gender
) {
}
