package dev.rimaz.clientele.bootstrap;

import dev.rimaz.clientele.customer.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

/**
 * The first admin account, bound from {@code clientele.bootstrap.admin.*}.
 * <p>
 * Nothing happens unless both the email and the password are set, so production
 * only gets an admin when {@code CLIENTELE_BOOTSTRAP_ADMIN_EMAIL} and
 * {@code CLIENTELE_BOOTSTRAP_ADMIN_PASSWORD} are provided.
 */
@Validated
@ConfigurationProperties(prefix = "clientele.bootstrap.admin")
public record AdminBootstrapProperties(
        @Email
        String email,

        @Size(min = 8, message = "must be at least 8 characters")
        String password,

        @DefaultValue("Admin")
        @NotBlank
        @Size(max = 15)
        String name,

        @DefaultValue("30")
        @NotNull
        @Min(16)
        @Max(100)
        Integer age,

        @DefaultValue("MALE")
        @NotNull
        Gender gender
) {

    public boolean isConfigured() {
        return StringUtils.hasText(email) && StringUtils.hasText(password);
    }

    public boolean isPartiallyConfigured() {
        return !isConfigured() && (StringUtils.hasText(email) || StringUtils.hasText(password));
    }
}
