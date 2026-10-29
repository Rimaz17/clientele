package dev.rimaz.clientele.customer;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

@Transactional
public interface CustomerRepository
        extends JpaRepository<Customer, Integer>, JpaSpecificationExecutor<Customer> {

    boolean existsCustomerByEmail(String email);
    boolean existsCustomerById(Integer id);
    Optional<Customer> findCustomerByEmail(String email);
    long countByRole(Role role);
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Customer c SET c.profileImageId = ?1 WHERE c.id = ?2")
    int updateProfileImageId(String profileImageId, Integer customerId);

    // Only for demo data: the database sets created_at for every real sign-up,
    // and the entity maps it read-only, hence the native query.
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE customer SET created_at = :createdAt WHERE id = :id", nativeQuery = true)
    int backdateCreatedAt(@Param("id") Integer customerId, @Param("createdAt") Instant createdAt);
}
