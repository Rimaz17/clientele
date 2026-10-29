package dev.rimaz.clientele.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JWTUtilTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-bytes-long";
    private static final String OTHER_SECRET = "another-secret-that-is-also-32-bytes-long";
    private static final String ISSUER = "clientele-test";
    private static final String SUBJECT = "alex@clientele.dev";

    private final JWTUtil underTest = jwtUtil(SECRET, Duration.ofDays(15), ISSUER);

    @Test
    void tokenIssuedWithTheConfiguredSecretIsValid() {
        String token = underTest.issueToken(SUBJECT, "ROLE_USER");

        assertThat(underTest.getSubject(token)).isEqualTo(SUBJECT);
        assertThat(underTest.isTokenValid(token, SUBJECT)).isTrue();
    }

    @Test
    void tokenIsNotValidForAnotherUser() {
        String token = underTest.issueToken(SUBJECT);

        assertThat(underTest.isTokenValid(token, "someone.else@clientele.dev")).isFalse();
    }

    @Test
    void tokenSignedWithADifferentSecretIsRejected() {
        String foreignToken = jwtUtil(OTHER_SECRET, Duration.ofDays(15), ISSUER).issueToken(SUBJECT);

        assertThatThrownBy(() -> underTest.getSubject(foreignToken))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void expiredTokenIsRejected() {
        String expiredToken = jwtUtil(SECRET, Duration.ofSeconds(-1), ISSUER).issueToken(SUBJECT);

        assertThatThrownBy(() -> underTest.isTokenValid(expiredToken, SUBJECT))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tokenCarriesTheConfiguredIssuerAndExpiration() {
        String token = underTest.issueToken(SUBJECT, List.of("ROLE_USER"));

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
        assertThat(claims.getSubject()).isEqualTo(SUBJECT);
        assertThat(claims.get("scopes", List.class)).containsExactly("ROLE_USER");
        Duration lifetime = Duration.between(
                claims.getIssuedAt().toInstant(), claims.getExpiration().toInstant());
        assertThat(lifetime).isEqualTo(Duration.ofDays(15));
    }

    private static JWTUtil jwtUtil(String secret, Duration expiration, String issuer) {
        return new JWTUtil(new JwtProperties(secret, expiration, issuer));
    }
}
