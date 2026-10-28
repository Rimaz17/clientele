package dev.rimaz.clientele.auth;

public record AuthenticationRequest(
        String username,
        String password
) {
}
