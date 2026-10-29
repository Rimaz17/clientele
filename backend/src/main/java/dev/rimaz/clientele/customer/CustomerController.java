package dev.rimaz.clientele.customer;

import dev.rimaz.clientele.jwt.JWTUtil;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final JWTUtil jwtUtil;

    public CustomerController(CustomerService customerService,
                              JWTUtil jwtUtil) {
        this.customerService = customerService;
        this.jwtUtil = jwtUtil;
    }

    // Binds page, size, sort, search, gender, minAge and maxAge from the query
    // string; invalid values are answered with 400 and field errors.
    @GetMapping
    public PageResponse<CustomerDTO> getCustomers(@Valid CustomerQuery query) {
        return customerService.getCustomers(query);
    }

    @GetMapping("{customerId}")
    public CustomerDTO getCustomer(
            @PathVariable("customerId") Integer customerId) {
        return customerService.getCustomer(customerId);
    }

    @SecurityRequirements
    @PostMapping
    public ResponseEntity<?> registerCustomer(
            @Valid @RequestBody CustomerRegistrationRequest request) {
        customerService.addCustomer(request);
        // Registration always creates a regular user; admins are bootstrapped or promoted.
        String jwtToken = jwtUtil.issueToken(request.email(), Role.USER.authority());
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, jwtToken)
                .build();
    }

    @DeleteMapping("{customerId}")
    public void deleteCustomer(
            @PathVariable("customerId") Integer customerId) {
        customerService.deleteCustomerById(customerId);
    }

    @PutMapping("{customerId}")
    public ResponseEntity<Void> updateCustomer(
            @PathVariable("customerId") Integer customerId,
            @Valid @RequestBody CustomerUpdateRequest updateRequest,
            @AuthenticationPrincipal Customer principal) {
        CustomerDTO updated = customerService.updateCustomer(customerId, updateRequest);
        if (isOwnEmailChange(principal, updated)) {
            String jwtToken = jwtUtil.issueToken(updated.username(), updated.roles());
            return ResponseEntity.ok()
                    .header(HttpHeaders.AUTHORIZATION, jwtToken)
                    .build();
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping(
            value = "{customerId}/profile-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public void uploadCustomerProfileImage(
            @PathVariable("customerId") Integer customerId,
            @RequestParam("file") MultipartFile file) {
        customerService.uploadCustomerProfileImage(customerId, file);
    }

    // Public, because <img> tags cannot send a token.
    @SecurityRequirements
    @GetMapping(
            value = "{customerId}/profile-image",
            produces = MediaType.IMAGE_JPEG_VALUE
    )
    public byte[] getCustomerProfileImage(
            @PathVariable("customerId") Integer customerId) {
        return customerService.getCustomerProfileImage(customerId);
    }

    // A token names its user by email, so users who change their own email need
    // a new token. Admins editing someone else keep their own.
    private static boolean isOwnEmailChange(Customer principal, CustomerDTO updated) {
        return principal.getId().equals(updated.id())
                && !principal.getEmail().equals(updated.email());
    }
}
