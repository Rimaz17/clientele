package dev.rimaz.clientele.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// A partial update: every field is optional, but a provided value must be valid.
public record CustomerUpdateRequest(
        @Size(min = 1, max = 15, message = "Name must be between 1 and 15 characters")
        @Pattern(regexp = ".*\\S.*", message = "Name must not be blank")
        String name,

        @Email(message = "Email must be a valid email address")
        @Size(max = 254, message = "Email must be at most 254 characters")
        String email,

        @Min(value = 16, message = "Age must be at least 16")
        @Max(value = 100, message = "Age must be at most 100")
        Integer age
) {
}
