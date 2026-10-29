package dev.rimaz.clientele.customer;

import dev.rimaz.clientele.exception.DuplicateResourceException;
import dev.rimaz.clientele.exception.RequestValidationException;
import dev.rimaz.clientele.exception.ResourceNotFoundException;
import dev.rimaz.clientele.s3.S3Buckets;
import dev.rimaz.clientele.s3.S3Service;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private static final Set<String> ALLOWED_PROFILE_IMAGE_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp"
    );

    // Admins may change any customer; everyone else only their own account.
    private static final String ADMIN_OR_SELF =
            "hasRole('ADMIN') or @customerAccess.isSelf(#customerId, authentication)";

    private final CustomerDao customerDao;
    private final CustomerDTOMapper customerDTOMapper;
    private final PasswordEncoder passwordEncoder;
    private final S3Service s3Service;
    private final S3Buckets s3Buckets;

    public CustomerService(@Qualifier("jdbc") CustomerDao customerDao,
                           CustomerDTOMapper customerDTOMapper,
                           PasswordEncoder passwordEncoder,
                           S3Service s3Service,
                           S3Buckets s3Buckets) {
        this.customerDao = customerDao;
        this.customerDTOMapper = customerDTOMapper;
        this.passwordEncoder = passwordEncoder;
        this.s3Service = s3Service;
        this.s3Buckets = s3Buckets;
    }

    public List<CustomerDTO> getAllCustomers() {
        return customerDao.selectAllCustomers()
                .stream()
                .map(customerDTOMapper)
                .collect(Collectors.toList());
    }

    public CustomerDTO getCustomer(Integer id) {
        return customerDao.selectCustomerById(id)
                .map(customerDTOMapper)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "customer with id [%s] not found".formatted(id)
                ));
    }

    public void addCustomer(CustomerRegistrationRequest customerRegistrationRequest) {
        // check if email exists
        String email = customerRegistrationRequest.email();
        if (customerDao.existsCustomerWithEmail(email)) {
            throw new DuplicateResourceException(
                    "email already taken"
            );
        }

        // add
        Customer customer = new Customer(
                customerRegistrationRequest.name(),
                customerRegistrationRequest.email(),
                passwordEncoder.encode(customerRegistrationRequest.password()),
                customerRegistrationRequest.age(),
                customerRegistrationRequest.gender());

        customerDao.insertCustomer(customer);
    }

    /**
     * Deletes a customer. Only admins and the customer themselves may do this;
     * anyone else gets an {@link org.springframework.security.access.AccessDeniedException}.
     */
    @PreAuthorize(ADMIN_OR_SELF)
    public void deleteCustomerById(Integer customerId) {
        checkIfCustomerExistsOrThrow(customerId);
        customerDao.deleteCustomerById(customerId);
    }

    private void checkIfCustomerExistsOrThrow(Integer customerId) {
        if (!customerDao.existsCustomerById(customerId)) {
            throw new ResourceNotFoundException(
                    "customer with id [%s] not found".formatted(customerId)
            );
        }
    }

    /**
     * Applies a partial update. Only admins and the customer themselves may do
     * this; anyone else gets an {@link org.springframework.security.access.AccessDeniedException}.
     */
    @PreAuthorize(ADMIN_OR_SELF)
    public void updateCustomer(Integer customerId,
                               CustomerUpdateRequest updateRequest) {
        // TODO: for JPA use .getReferenceById(customerId) as it does does not bring object into memory and instead a reference
        Customer customer = customerDao.selectCustomerById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "customer with id [%s] not found".formatted(customerId)
                ));

        boolean changes = false;

        if (updateRequest.name() != null && !updateRequest.name().equals(customer.getName())) {
            customer.setName(updateRequest.name());
            changes = true;
        }

        if (updateRequest.age() != null && !updateRequest.age().equals(customer.getAge())) {
            customer.setAge(updateRequest.age());
            changes = true;
        }

        if (updateRequest.email() != null && !updateRequest.email().equals(customer.getEmail())) {
            if (customerDao.existsCustomerWithEmail(updateRequest.email())) {
                throw new DuplicateResourceException(
                        "email already taken"
                );
            }
            customer.setEmail(updateRequest.email());
            changes = true;
        }

        if (!changes) {
           throw new RequestValidationException("no data changes found");
        }

        customerDao.updateCustomer(customer);
    }

    /**
     * Stores a new profile image. Only admins and the customer themselves may do
     * this; anyone else gets an {@link org.springframework.security.access.AccessDeniedException}.
     */
    @PreAuthorize(ADMIN_OR_SELF)
    public void uploadCustomerProfileImage(Integer customerId,
                                           MultipartFile file) {
        checkIfCustomerExistsOrThrow(customerId);
        validateProfileImage(file);
        String profileImageId = UUID.randomUUID().toString();
        try {
            s3Service.putObject(
                    s3Buckets.getCustomer(),
                    "profile-images/%s/%s".formatted(customerId, profileImageId),
                    file.getBytes()
            );
        } catch (IOException e) {
            throw new RuntimeException("failed to upload profile image", e);
        }
        customerDao.updateCustomerProfileImageId(profileImageId, customerId);
    }

    // The content type is declared by the client, so this is a sanity check
    // against mistakes rather than proof that the bytes are an image.
    private void validateProfileImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RequestValidationException("profile image file is empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_PROFILE_IMAGE_TYPES.contains(contentType)) {
            throw new RequestValidationException(
                    "profile image must be a JPEG, PNG or WebP file"
            );
        }
    }

    public byte[] getCustomerProfileImage(Integer customerId) {
        var customer = customerDao.selectCustomerById(customerId)
                .map(customerDTOMapper)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "customer with id [%s] not found".formatted(customerId)
                ));

        if (StringUtils.isBlank(customer.profileImageId())) {
            throw new ResourceNotFoundException(
                    "customer with id [%s] profile image not found".formatted(customerId));
        }

        byte[] profileImage = s3Service.getObject(
                s3Buckets.getCustomer(),
                "profile-images/%s/%s".formatted(customerId, customer.profileImageId())
        );
        return profileImage;
    }
}

