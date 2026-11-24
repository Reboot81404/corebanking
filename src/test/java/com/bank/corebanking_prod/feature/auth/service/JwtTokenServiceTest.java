package com.bank.corebanking_prod.feature.auth.service;

import com.bank.corebanking_prod.feature.auth.model.enums.UserRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceTest {

    private static final String SECRET = "VerySecureSecretKeyForTestingJwtTokenServiceAtLeast256BitsLong!";
    private static final String ISSUER = "corebanking-prod";
    private static final long EXPIRATION_MS = 900_000L;

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService(SECRET, EXPIRATION_MS, ISSUER);
    }

    @Test
    @DisplayName("JWT contains issuer, subject, role, issued-at and expiry claims")
    void shouldGenerateAndValidateToken() {
        String token = jwtTokenService.generateToken("testuser", UserRole.CUSTOMER);

        assertTrue(jwtTokenService.validateToken(token));
        assertEquals("testuser", jwtTokenService.getUsernameFromToken(token));
        assertEquals(UserRole.CUSTOMER, jwtTokenService.getRoleFromToken(token));
    }

    @Test
    @DisplayName("Malformed and tampered JWTs are rejected")
    void shouldRejectMalformedAndTamperedTokens() {
        String token = jwtTokenService.generateToken("testuser", UserRole.CUSTOMER);
        assertFalse(jwtTokenService.validateToken(token + "xyz"));
        assertFalse(jwtTokenService.validateToken("invalid.token.structure"));
        assertFalse(jwtTokenService.validateToken(null));
        assertFalse(jwtTokenService.validateToken(""));
    }

    @Test
    @DisplayName("JWT signed with a different key or issuer is rejected")
    void shouldRejectWrongKeyAndIssuer() {
        JwtTokenService anotherKeyService = new JwtTokenService(
                "AnotherSecureSecretKeyForTestingJwtTokenServiceAtLeast256BitsLong!",
                EXPIRATION_MS,
                ISSUER
        );
        String wrongKeyToken = anotherKeyService.generateToken("testuser", UserRole.CUSTOMER);
        assertFalse(jwtTokenService.validateToken(wrongKeyToken));

        String wrongIssuerToken = Jwts.builder()
                .issuer("untrusted-issuer")
                .subject("testuser")
                .claim("role", UserRole.CUSTOMER.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(signingKey())
                .compact();
        assertFalse(jwtTokenService.validateToken(wrongIssuerToken));
    }

    @Test
    @DisplayName("JWT with an unsupported role claim is rejected")
    void shouldRejectUnsupportedRoleClaim() {
        String token = Jwts.builder()
                .issuer(ISSUER)
                .subject("testuser")
                .claim("role", "SUPERADMIN")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(signingKey())
                .compact();

        assertFalse(jwtTokenService.validateToken(token));
    }

    @Test
    @DisplayName("Expired tokens are rejected")
    void shouldRejectExpiredToken() {
        Date now = new Date();
        String expiredToken = Jwts.builder()
                .issuer(ISSUER)
                .subject("expireduser")
                .claim("role", UserRole.CUSTOMER.name())
                .issuedAt(new Date(now.getTime() - 10_000))
                .expiration(new Date(now.getTime() - 1_000))
                .signWith(signingKey())
                .compact();

        assertFalse(jwtTokenService.validateToken(expiredToken));
    }

    @Test
    @DisplayName("Missing, short, or invalid token configuration fails fast")
    void shouldFailFastOnInvalidConfiguration() {
        assertThrows(IllegalStateException.class, () -> new JwtTokenService("", EXPIRATION_MS, ISSUER));
        assertThrows(IllegalStateException.class, () -> new JwtTokenService("too-short", EXPIRATION_MS, ISSUER));
        assertThrows(IllegalStateException.class, () -> new JwtTokenService(SECRET, 0, ISSUER));
        assertThrows(IllegalStateException.class, () -> new JwtTokenService(SECRET, EXPIRATION_MS, " "));
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }
}
