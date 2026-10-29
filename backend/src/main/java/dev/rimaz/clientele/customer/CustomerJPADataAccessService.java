package dev.rimaz.clientele.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("jpa")
public class CustomerJPADataAccessService implements CustomerDao {

    private final CustomerRepository customerRepository;

    public CustomerJPADataAccessService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public PageResponse<Customer> selectCustomers(CustomerQuery query) {
        CustomerSortField field = query.sortField();
        Sort sort = Sort.by(query.sortDirection(), field.property());
        // Same id tie-breaker as the JDBC implementation, for stable pages.
        if (field != CustomerSortField.ID) {
            sort = sort.and(Sort.by(CustomerSortField.ID.property()));
        }
        Page<Customer> page = customerRepository.findAll(
                CustomerSpecifications.matching(query),
                PageRequest.of(query.page(), query.size(), sort)
        );
        return PageResponse.of(page.getContent(), query.page(), query.size(), page.getTotalElements());
    }

    @Override
    public Optional<Customer> selectCustomerById(Integer id) {
        return customerRepository.findById(id);
    }

    @Override
    public void insertCustomer(Customer customer) {
        customerRepository.save(customer);
    }

    @Override
    public boolean existsCustomerWithEmail(String email) {
        return customerRepository.existsCustomerByEmail(email);
    }

    @Override
    public boolean existsCustomerById(Integer id) {
        return customerRepository.existsCustomerById(id);
    }

    @Override
    public void deleteCustomerById(Integer customerId) {
        customerRepository.deleteById(customerId);
    }

    @Override
    public void updateCustomer(Customer update) {
        customerRepository.save(update);
    }

    @Override
    public Optional<Customer> selectUserByEmail(String email) {
        return customerRepository.findCustomerByEmail(email);
    }

    @Override
    public void updateCustomerProfileImageId(String profileImageId,
                                             Integer customerId) {
        customerRepository.updateProfileImageId(profileImageId, customerId);
    }

}
