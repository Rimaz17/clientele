package dev.rimaz.clientele.stats;

import dev.rimaz.clientele.exception.ApiError;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// A literal path segment is more specific than CustomerController's
// {customerId}, so Spring routes /customers/stats here.
@Tag(name = "Statistics", description = "Aggregate customer statistics for the dashboard")
@RestController
@RequestMapping("api/v1/customers/stats")
public class CustomerStatsController {

    private final CustomerStatsService customerStatsService;

    public CustomerStatsController(CustomerStatsService customerStatsService) {
        this.customerStatsService = customerStatsService;
    }

    @Operation(summary = "Get dashboard statistics",
            description = "Totals, the gender split, age ranges and sign-ups per UTC month for the last "
                    + "12 months, computed in SQL. Aggregates only, no personal data. Any signed-in user may call it.")
    @ApiResponse(responseCode = "200", description = "The statistics")
    @ApiResponse(responseCode = "403", description = "Missing, invalid or expired token",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiError.class)))
    @GetMapping
    public CustomerStats getCustomerStats() {
        return customerStatsService.getCustomerStats();
    }
}
