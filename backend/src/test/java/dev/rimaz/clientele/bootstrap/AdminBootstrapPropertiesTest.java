package dev.rimaz.clientele.bootstrap;

import dev.rimaz.clientele.customer.Gender;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Loads the real application.yml (and application-dev.yml when the dev profile
 * is active) to prove which environments get a bootstrap admin.
 */
class AdminBootstrapPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(AdminBootstrapPropertiesConfiguration.class);

    @Test
    void devProfileConfiguresADevelopmentAdmin() {
        contextRunner.run(context -> {
            AdminBootstrapProperties properties = context.getBean(AdminBootstrapProperties.class);
            assertThat(properties.isConfigured()).isTrue();
            assertThat(properties.email()).isEqualTo("admin@clientele.dev");
        });
    }

    @Test
    void otherProfilesHaveNoAdminByDefault() {
        contextRunner
                .withPropertyValues("spring.profiles.active=prod")
                .run(context -> {
                    AdminBootstrapProperties properties = context.getBean(AdminBootstrapProperties.class);
                    assertThat(properties.isConfigured()).isFalse();
                    assertThat(properties.isPartiallyConfigured()).isFalse();
                    assertThat(properties.name()).isEqualTo("Admin");
                    assertThat(properties.age()).isEqualTo(30);
                    assertThat(properties.gender()).isEqualTo(Gender.MALE);
                });
    }

    @Test
    void anAdminCanBeConfiguredFromTheEnvironment() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "clientele.bootstrap.admin.email=owner@clientele.dev",
                        "clientele.bootstrap.admin.password=a-long-password")
                .run(context -> {
                    AdminBootstrapProperties properties = context.getBean(AdminBootstrapProperties.class);
                    assertThat(properties.isConfigured()).isTrue();
                    assertThat(properties.email()).isEqualTo("owner@clientele.dev");
                });
    }

    @Test
    void startupFailsWithAShortAdminPassword() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "clientele.bootstrap.admin.email=owner@clientele.dev",
                        "clientele.bootstrap.admin.password=short")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(BindValidationException.class)
                            .rootCause()
                            .hasMessageContaining("at least 8 characters");
                });
    }

    @EnableConfigurationProperties(AdminBootstrapProperties.class)
    static class AdminBootstrapPropertiesConfiguration {
    }
}
