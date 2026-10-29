package dev.rimaz.clientele.customer;

public enum Role {
    USER,
    ADMIN;

    // Spring Security expects role authorities to carry the ROLE_ prefix.
    public String authority() {
        return "ROLE_" + name();
    }
}
