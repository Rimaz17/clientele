package dev.rimaz.clientele.customer;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CustomerJPAQueryTest extends AbstractCustomerQueryTest {

    @Autowired
    private CustomerRepository customerRepository;

    private CustomerJPADataAccessService underTest;

    @BeforeEach
    void setUp() {
        underTest = new CustomerJPADataAccessService(customerRepository);
    }

    @Override
    protected CustomerDao dao() {
        return underTest;
    }
}
