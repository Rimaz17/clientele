package dev.rimaz.clientele.customer;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Ownership rules used by method security expressions such as
 * {@code @customerAccess.isSelf(#customerId, authentication)}.
 */
@Component("customerAccess")
public class CustomerAccess {

    private final CustomerDao customerDao;

    public CustomerAccess(@Qualifier("jdbc") CustomerDao customerDao) {
        this.customerDao = customerDao;
    }

    /**
     * Whether the authenticated user is the customer with the given id. Users
     * are identified by email, compared case-insensitively. An unknown id is
     * never "self", so non-admins get 403 rather than learning whether it exists.
     */
    public boolean isSelf(Integer customerId, Authentication authentication) {
        if (customerId == null || authentication == null || authentication.getName() == null) {
            return false;
        }
        return customerDao.selectCustomerById(customerId)
                .map(customer -> customer.getEmail().equalsIgnoreCase(authentication.getName()))
                .orElse(false);
    }
}
