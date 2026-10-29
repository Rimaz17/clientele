package dev.rimaz.clientele.journey;

import org.springframework.boot.test.context.SpringBootTest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Starts the whole application on a random port against the local database.
 * Demo data seeding is switched off, so the tests only see data they create.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = "clientele.seed.enabled=false")
public @interface IntegrationTest {
}
