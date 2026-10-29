package dev.rimaz.clientele.journey;

import dev.rimaz.clientele.customer.CustomerRegistrationRequest;
import dev.rimaz.clientele.customer.Gender;
import dev.rimaz.clientele.stats.AgeRangeCount;
import dev.rimaz.clientele.stats.CustomerStats;
import dev.rimaz.clientele.stats.MonthlySignups;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@IntegrationTest
class CustomerStatsIT {

    private static final String STATS_PATH = "/api/v1/customers/stats";

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void returnsTheStatsShapeToASignedInUser() {
        YearMonth monthBefore = YearMonth.now(ZoneOffset.UTC);
        String bearerToken = register();

        CustomerStats stats = webTestClient.get()
                .uri(STATS_PATH)
                .header(AUTHORIZATION, bearerToken)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(CustomerStats.class)
                .returnResult()
                .getResponseBody();
        YearMonth monthAfter = YearMonth.now(ZoneOffset.UTC);

        // The database is shared with other tests, so only lower bounds are known.
        assertThat(stats).isNotNull();
        assertThat(stats.totalCustomers()).isPositive();
        assertThat(stats.newLast30Days()).isPositive().isLessThanOrEqualTo(stats.totalCustomers());
        assertThat(stats.averageAge()).isBetween(16.0, 100.0);
        assertThat(stats.genderSplit()).containsOnlyKeys(Gender.MALE, Gender.FEMALE);
        assertThat(stats.genderSplit().values().stream().mapToLong(Long::longValue).sum())
                .isEqualTo(stats.totalCustomers());
        assertThat(stats.ageDistribution()).extracting(AgeRangeCount::range)
                .containsExactly("16-24", "25-34", "35-44", "45-54", "55-64", "65+");
        assertThat(stats.ageDistribution().stream().mapToLong(AgeRangeCount::count).sum())
                .isEqualTo(stats.totalCustomers());

        List<MonthlySignups> signups = stats.signupsByMonth();
        assertThat(signups).hasSize(12);
        YearMonth lastMonth = YearMonth.parse(signups.get(11).month());
        assertThat(lastMonth).isIn(monthBefore, monthAfter);
        for (int i = 0; i < signups.size(); i++) {
            assertThat(signups.get(i).month()).isEqualTo(lastMonth.minusMonths(11 - i).toString());
        }
        assertThat(signups.get(11).count()).isPositive();
    }

    @Test
    void returnsTheExactJsonFieldNames() {
        webTestClient.get()
                .uri(STATS_PATH)
                .header(AUTHORIZATION, register())
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .jsonPath("$.totalCustomers").isNumber()
                .jsonPath("$.newLast30Days").isNumber()
                .jsonPath("$.averageAge").isNumber()
                .jsonPath("$.genderSplit.MALE").isNumber()
                .jsonPath("$.genderSplit.FEMALE").isNumber()
                .jsonPath("$.ageDistribution[0].range").isEqualTo("16-24")
                .jsonPath("$.ageDistribution[0].count").isNumber()
                .jsonPath("$.signupsByMonth.length()").isEqualTo(12)
                .jsonPath("$.signupsByMonth[0].month").value(matchesPattern("[0-9]{4}-[0-9]{2}"))
                .jsonPath("$.signupsByMonth[0].count").isNumber();
    }

    @Test
    void requiresAuthentication() {
        webTestClient.get()
                .uri(STATS_PATH)
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    private String register() {
        String email = "stats-" + UUID.randomUUID() + "@clientele.dev";
        var request = new CustomerRegistrationRequest("Stats", email, "password", 30, Gender.FEMALE);
        String token = webTestClient.post()
                .uri("/api/v1/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(request), CustomerRegistrationRequest.class)
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult(Void.class)
                .getResponseHeaders()
                .getFirst(AUTHORIZATION);
        return "Bearer " + token;
    }
}
