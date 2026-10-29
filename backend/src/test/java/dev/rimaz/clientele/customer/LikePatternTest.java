package dev.rimaz.clientele.customer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LikePatternTest {

    @Test
    void wrapsPlainTextInWildcards() {
        assertThat(LikePattern.contains("ali")).isEqualTo("%ali%");
    }

    @Test
    void escapesWildcardsSoTheyMatchLiterally() {
        assertThat(LikePattern.contains("50%_off")).isEqualTo("%50\\%\\_off%");
    }

    @Test
    void escapesTheEscapeCharacterFirst() {
        assertThat(LikePattern.contains("a\\%")).isEqualTo("%a\\\\\\%%");
    }
}
