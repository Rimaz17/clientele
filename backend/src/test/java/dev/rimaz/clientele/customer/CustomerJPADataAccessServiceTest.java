package dev.rimaz.clientele.customer;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerJPADataAccessServiceTest {

    private CustomerJPADataAccessService underTest;
    private AutoCloseable autoCloseable;
    @Mock private CustomerRepository customerRepository;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        underTest = new CustomerJPADataAccessService(customerRepository);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void selectCustomersRequestsThePageAndSortWithAnIdTieBreaker() {
        // Given
        List<Customer> customers = List.of(new Customer());
        when(customerRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(customers, PageRequest.of(2, 10), 21));
        CustomerQuery query = new CustomerQuery(2, 10, "name,desc", null, null, null, null);

        // When
        PageResponse<Customer> actual = underTest.selectCustomers(query);

        // Then
        assertThat(actual).isEqualTo(new PageResponse<>(customers, 2, 10, 21, 3));
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(customerRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue()).isEqualTo(
                PageRequest.of(2, 10, Sort.by(Sort.Direction.DESC, "name").and(Sort.by("id"))));
    }

    @Test
    void selectCustomerById() {
        // Given
        int id = 1;

        // When
        underTest.selectCustomerById(id);

        // Then
        verify(customerRepository).findById(id);
    }

    @Test
    void insertCustomer() {
        // Given
        Customer customer = new Customer(
                1, "Ali", "ali@gmail.com", "password", 2,
                Gender.MALE);

        // When
        underTest.insertCustomer(customer);

        // Then
        verify(customerRepository).save(customer);
    }

    @Test
    void existsCustomerWithEmail() {
        // Given
        String email = "foo@gmail.com";

        // When
        underTest.existsCustomerWithEmail(email);

        // Then
        verify(customerRepository).existsCustomerByEmail(email);
    }

    @Test
    void existsCustomerById() {
        // Given
        int id = 1;

        // When
        underTest.existsCustomerById(id);

        // Then
        verify(customerRepository).existsCustomerById(id);
    }

    @Test
    void deleteCustomerById() {
        // Given
        int id = 1;

        // When
        underTest.deleteCustomerById(id);

        // Then
        verify(customerRepository).deleteById(id);
    }

    @Test
    void updateCustomer() {
        // Given
        Customer customer = new Customer(
                1, "Ali", "ali@gmail.com", "password", 2,
                Gender.MALE);

        // When
        underTest.updateCustomer(customer);

        // Then
        verify(customerRepository).save(customer);
    }

    @Test
    void canUpdateProfileImageId() {
        // Given
        String profileImageId = "2222";
        Integer customerId = 1;

        // When
        underTest.updateCustomerProfileImageId(profileImageId, customerId);

        // Then
        verify(customerRepository).updateProfileImageId(profileImageId, customerId);
    }
}