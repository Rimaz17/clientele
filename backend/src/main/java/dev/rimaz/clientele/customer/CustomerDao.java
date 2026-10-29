package dev.rimaz.clientele.customer;

import java.util.Optional;

public interface CustomerDao {
    PageResponse<Customer> selectCustomers(CustomerQuery query);
    Optional<Customer> selectCustomerById(Integer customerId);
    void insertCustomer(Customer customer);
    boolean existsCustomerWithEmail(String email);
    boolean existsCustomerById(Integer customerId);
    void deleteCustomerById(Integer customerId);
    void updateCustomer(Customer update);
    Optional<Customer> selectUserByEmail(String email);
    void updateCustomerProfileImageId(String profileImageId, Integer customerId);
}
