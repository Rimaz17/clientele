package dev.rimaz.clientele.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loads the real application.yml (and application-dev.yml when the dev profile
 * is active) to prove how the JWT secret is resolved per profile.
 */
class JwtPropertiesTest {

    private static final String VALID_SECRET = "a-production-like-secret-of-at-least-32-bytes";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(JwtPropertiesConfiguration.class);

    @Test
    void devProfileIsTheDefaultAndProvidesASecret() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getEnvironment().getDefaultProfiles()).containsExactly("dev");

            JwtProperties properties = context.getBean(JwtProperties.class);
            assertThat(properties.secret()).startsWith("dev-only-");
            assertThat(properties.isSecretLongEnough()).isTrue();
            assertThat(properties.issuer()).isEqualTo("clientele");
            assertThat(properties.expiration()).isEqualTo(Duration.ofDays(15));
        });
    }

    @Test
    void startupFailsWithoutASecretOutsideTheDevProfile() {
        contextRunner
                .withPropertyValues("spring.profiles.active=prod")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(BindValidationException.class)
                            .rootCause()
                            .hasMessageContaining("clientele.jwt")
                            .hasMessageContaining("CLIENTELE_JWT_SECRET");
                });
    }

    @Test
    void startupFailsWhenTheSecretIsTooShort() {
        contextRunner
                .withPropertyValues("spring.profiles.active=prod", "clientele.jwt.secret=too-short")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(BindValidationException.class)
                            .rootCause()
                            .hasMessageContaining("at least 32 bytes");
                });
    }

    @Test
    void startupSucceedsWithASecretFromTheEnvironment() {
        contextRunner
                .withPropertyValues("spring.profiles.active=prod", "clientele.jwt.secret=" + VALID_SECRET)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(JwtProperties.class).secret()).isEqualTo(VALID_SECRET);
                });
    }

    @EnableConfigurationProperties(JwtProperties.class)
    static class JwtPropertiesConfiguration {
    }
}
