package dev.rimaz.clientele.auth;

import dev.rimaz.clientele.customer.CustomerDTO;
import io.swagger.v3.oas.annotations.media.Schema;

public record AuthenticationResponse (
        @Schema(description = "A JWT to send as Authorization: Bearer <token>",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbGV4LnNtaXRoQGV4YW1wbGUuY29tIn0.signature")
        String token,
        @Schema(description = "The customer who logged in")
        CustomerDTO customerDTO){
}
