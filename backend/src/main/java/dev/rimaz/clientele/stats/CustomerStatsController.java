package dev.rimaz.clientele.stats;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// A literal path segment is more specific than CustomerController's
// {customerId}, so Spring routes /customers/stats here.
@RestController
@RequestMapping("api/v1/customers/stats")
public class CustomerStatsController {

    private final CustomerStatsService customerStatsService;

    public CustomerStatsController(CustomerStatsService customerStatsService) {
        this.customerStatsService = customerStatsService;
    }

    @GetMapping
    public CustomerStats getCustomerStats() {
        return customerStatsService.getCustomerStats();
    }
}
