package com.ecomera.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String SIGNING_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private JwtUtil jwtUtil;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SIGNING_KEY);
        secretKey = Keys.hmacShaKeyFor(SIGNING_KEY.getBytes(StandardCharsets.UTF_8));
    }

    private String createToken(String subject, String userId, String roles, long ttl) {
        return Jwts.builder()
                .subject(subject)
                .claim("userId", userId)
                .claim("roles", roles)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + ttl))
                .signWith(secretKey)
                .compact();
    }

    @Test
    void validateToken_shouldReturnTrue_forValidToken() {
        String token = createToken("test@example.com", UUID.randomUUID().toString(), "USER", 3600000);
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_shouldReturnFalse_forExpiredToken() {
        String token = createToken("test@example.com", UUID.randomUUID().toString(), "USER", -3600000);
        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_shouldReturnFalse_forInvalidSignature() {
        SecretKey wrongKey = Keys.hmacShaKeyFor("AnotherKeyThatIsDifferentAndAlso256BitsLongForTesting!!".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("test@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(wrongKey)
                .compact();
        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_shouldReturnFalse_forMalformedToken() {
        assertThat(jwtUtil.validateToken("malformed.token.here")).isFalse();
    }

    @Test
    void validateToken_shouldReturnFalse_forEmptyString() {
        assertThat(jwtUtil.validateToken("")).isFalse();
    }

    @Test
    void extractEmail_shouldReturnSubject() {
        String token = createToken("user@example.com", UUID.randomUUID().toString(), "USER", 3600000);
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("user@example.com");
    }

    @Test
    void extractUserId_shouldReturnUUID() {
        UUID userId = UUID.randomUUID();
        String token = createToken("test@example.com", userId.toString(), "USER", 3600000);
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    void extractUserId_shouldThrowException_whenClaimMissing() {
        String token = Jwts.builder()
                .subject("test@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(secretKey)
                .compact();

        assertThatThrownBy(() -> jwtUtil.extractUserId(token))
                .isInstanceOf(Exception.class);
    }

    @Test
    void extractRoles_shouldReturnRolesClaim() {
        String token = createToken("test@example.com", UUID.randomUUID().toString(), "ADMIN,MANAGER", 3600000);
        assertThat(jwtUtil.extractRoles(token)).isEqualTo("ADMIN,MANAGER");
    }

    @Test
    void extractRoles_shouldReturnNull_whenClaimMissing() {
        String token = Jwts.builder()
                .subject("test@example.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(secretKey)
                .compact();

        assertThat(jwtUtil.extractRoles(token)).isNull();
    }

    @Test
    void constructor_shouldAcceptValidKey() {
        JwtUtil util = new JwtUtil(SIGNING_KEY);
        assertThat(util).isNotNull();
    }
}
