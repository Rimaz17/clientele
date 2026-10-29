package dev.rimaz.clientele.customer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseTest {

    @Test
    void computesTheNumberOfPages() {
        assertThat(PageResponse.of(List.of(), 0, 20, 0).totalPages()).isZero();
        assertThat(PageResponse.of(List.of(), 0, 20, 1).totalPages()).isEqualTo(1);
        assertThat(PageResponse.of(List.of(), 0, 20, 20).totalPages()).isEqualTo(1);
        assertThat(PageResponse.of(List.of(), 0, 20, 21).totalPages()).isEqualTo(2);
        assertThat(PageResponse.of(List.of(), 6, 20, 132).totalPages()).isEqualTo(7);
    }

    @Test
    void mapsTheContentAndKeepsThePageDetails() {
        PageResponse<Integer> page = PageResponse.of(List.of(1, 2), 1, 2, 5);

        PageResponse<String> mapped = page.map(number -> "#" + number);

        assertThat(mapped).isEqualTo(new PageResponse<>(List.of("#1", "#2"), 1, 2, 5, 3));
    }
}
