package dev.rimaz.clientele.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// The rules mirror the React sign-up form so both layers agree.
public record CustomerRegistrationRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 15, message = "Name must be at most 15 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 254, message = "Email must be at most 254 characters")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 4, max = 15, message = "Password must be between 4 and 15 characters")
        String password,

        @NotNull(message = "Age is required")
        @Min(value = 16, message = "Age must be at least 16")
        @Max(value = 100, message = "Age must be at most 100")
        Integer age,

        @NotNull(message = "Gender is required")
        Gender gender
) {
}
