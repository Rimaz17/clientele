package dev.rimaz.clientele.auth;

import dev.rimaz.clientele.customer.CustomerDTO;

public record AuthenticationResponse (
        String token,
        CustomerDTO customerDTO){
}
