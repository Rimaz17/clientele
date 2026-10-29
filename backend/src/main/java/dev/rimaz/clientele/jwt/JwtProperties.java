package dev.rimaz.clientele.jwt;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * JWT settings bound from {@code clientele.jwt.*}.
 * <p>
 * The secret has no default. The {@code dev} profile supplies a development-only
 * value; every other environment must set {@code CLIENTELE_JWT_SECRET}, otherwise
 * the application refuses to start.
 */
@Validated
@ConfigurationProperties(prefix = "clientele.jwt")
public record JwtProperties(
        @NotBlank(message = "must be set, for example with the CLIENTELE_JWT_SECRET environment variable")
        String secret,

        @NotNull
        Duration expiration,

        @NotBlank
        String issuer
) {

    // HS256 needs a key of at least 256 bits, and jjwt rejects anything shorter.
    private static final int MIN_SECRET_BYTES = 32;

    @AssertTrue(message = "clientele.jwt.secret must be at least 32 bytes (256 bits) long")
    public boolean isSecretLongEnough() {
        return secret == null || secret.getBytes(StandardCharsets.UTF_8).length >= MIN_SECRET_BYTES;
    }
}
