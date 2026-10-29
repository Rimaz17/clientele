package dev.rimaz.clientele.customer;

import dev.rimaz.clientele.exception.ApiError;
import dev.rimaz.clientele.jwt.JWTUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@Tag(name = "Customers", description = "Sign up, list, view, update and delete customers, and their profile pictures")
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
    @Operation(summary = "List customers",
            description = "One page of customers, filtered, searched and sorted. Any signed-in user may call it.")
    @ApiResponse(responseCode = "200", description = "One page of customers")
    @ApiResponse(responseCode = "400",
            description = "A query parameter is invalid; `errors` names each one",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "Missing, invalid or expired token",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiError.class)))
    @GetMapping
    public PageResponse<CustomerDTO> getCustomers(@ParameterObject @Valid CustomerQuery query) {
        return customerService.getCustomers(query);
    }

    @Operation(summary = "Get a customer")
    @ApiResponse(responseCode = "200", description = "The customer")
    @ApiResponse(responseCode = "400", description = "The customer id is not a number",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "Missing, invalid or expired token",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "No customer has this id",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiError.class)))
    @GetMapping("{customerId}")
    public CustomerDTO getCustomer(
            @Parameter(description = "The customer's id", example = "1")
            @PathVariable("customerId") Integer customerId) {
        return customerService.getCustomer(customerId);
    }

    @Operation(summary = "Sign up",
            description = "Creates a regular user (never an admin) and returns a token for it. No token needed.")
    @ApiResponse(responseCode = "200", description = "Signed up",
            headers = @Header(name = HttpHeaders.AUTHORIZATION,
                    description = "A JWT for the new customer",
                    schema = @Schema(type = "string")))
    @ApiResponse(responseCode = "400",
            description = "The body is malformed or a field is invalid; `errors` names each field",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "The email is already taken",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiError.class)))
    @SecurityRequirements
    @PostMapping
    public ResponseEntity<Void> registerCustomer(
            @Valid @RequestBody CustomerRegistrationRequest request) {
        customerService.addCustomer(request);
        // Registration always creates a regular user; admins are bootstrapped or promoted.
        String jwtToken = jwtUtil.issueToken(request.email(), Role.USER.authority());
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, jwtToken)
                .build();
    }

    @Operation(summary = "Delete a customer",
            description = "Admins may delete any customer; everyone else only their own account.")
    @ApiResponse(responseCode = "200", description = "Deleted")
    @ApiResponse(responseCode = "400", description = "The customer id is not a number",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403",
            description = "Missing, invalid or expired token, or the caller is neither an admin nor this customer",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "No customer has this id",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiError.class)))
    @DeleteMapping("{customerId}")
    public void deleteCustomer(
            @Parameter(description = "The customer's id", example = "1")
            @PathVariable("customerId") Integer customerId) {
        customerService.deleteCustomerById(customerId);
    }

    @Operation(summary = "Update a customer",
            description = "Changes the fields that are present. Admins may update any customer; "
                    + "everyone else only their own account.")
    @ApiResponse(responseCode = "200", description = "Updated",
            headers = @Header(name = HttpHeaders.AUTHORIZATION,
                    description = "A new JWT, sent only when users change their own email, "
                            + "because the old token names the old email",
                    schema = @Schema(type = "string")))
    @ApiResponse(responseCode = "400",
            description = "The body is malformed, a field is invalid, or nothing would change",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403",
            description = "Missing, invalid or expired token, or the caller is neither an admin nor this customer",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "No customer has this id",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "409", description = "The new email is already taken",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiError.class)))
    @PutMapping("{customerId}")
    public ResponseEntity<Void> updateCustomer(
            @Parameter(description = "The customer's id", example = "1")
            @PathVariable("customerId") Integer customerId,
            @Valid @RequestBody CustomerUpdateRequest updateRequest,
            @Parameter(hidden = true)
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

    @Operation(summary = "Upload a profile picture",
            description = "A JPEG, PNG or WebP file of at most 10 MB in the `file` part. "
                    + "Admins may upload for any customer; everyone else only for their own account.")
    @ApiResponse(responseCode = "200", description = "Uploaded")
    @ApiResponse(responseCode = "400",
            description = "The customer id is not a number, the `file` part is missing or empty, "
                    + "or it is not a JPEG, PNG or WebP image",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403",
            description = "Missing, invalid or expired token, or the caller is neither an admin nor this customer",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "No customer has this id",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "413", description = "The file is larger than 10 MB",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiError.class)))
    @PostMapping(
            value = "{customerId}/profile-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public void uploadCustomerProfileImage(
            @Parameter(description = "The customer's id", example = "1")
            @PathVariable("customerId") Integer customerId,
            @Parameter(description = "The image: JPEG, PNG or WebP, at most 10 MB")
            @RequestParam("file") MultipartFile file) {
        customerService.uploadCustomerProfileImage(customerId, file);
    }

    // Public, because <img> tags cannot send a token.
    @Operation(summary = "Download a profile picture",
            description = "No token needed, so `<img>` tags can load it.")
    @ApiResponse(responseCode = "200", description = "The image")
    @ApiResponse(responseCode = "400", description = "The customer id is not a number",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "No customer has this id, or the customer has no picture",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiError.class)))
    @SecurityRequirements
    @GetMapping(
            value = "{customerId}/profile-image",
            produces = MediaType.IMAGE_JPEG_VALUE
    )
    public byte[] getCustomerProfileImage(
            @Parameter(description = "The customer's id", example = "1")
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
