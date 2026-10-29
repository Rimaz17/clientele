package dev.rimaz.clientele.customer;

import org.junit.jupiter.api.BeforeEach;

class CustomerJDBCQueryTest extends AbstractCustomerQueryTest {

    private CustomerJDBCDataAccessService underTest;

    @BeforeEach
    void setUp() {
        underTest = new CustomerJDBCDataAccessService(getJdbcTemplate(), new CustomerRowMapper());
    }

    @Override
    protected CustomerDao dao() {
        return underTest;
    }
}
