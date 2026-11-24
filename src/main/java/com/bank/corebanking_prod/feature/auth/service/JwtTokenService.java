package com.bank.corebanking_prod.feature.auth.service;

import com.bank.corebanking_prod.feature.auth.model.entity.UserIdentityEntity;
import com.bank.corebanking_prod.feature.auth.model.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtTokenService {

    private static final String CLAIM_ROLE = "role";

    private final SecretKey signingKey;
    private final long expirationMs;
    private final String issuer;

    public JwtTokenService(
            @Value("${jwt.secret:}") String secret,
            @Value("${jwt.expiration-ms:900000}") long expirationMs,
            @Value("${jwt.issuer:corebanking-prod}") String issuer) {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("JWT_SECRET must be configured before the application can start.");
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 UTF-8 bytes.");
        }
        if (expirationMs <= 0) {
            throw new IllegalStateException("JWT expiration must be a positive number of milliseconds.");
        }
        if (!StringUtils.hasText(issuer)) {
            throw new IllegalStateException("JWT issuer must not be blank.");
        }

        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
        this.issuer = issuer;
    }

    public String generateToken(UserIdentityEntity user) {
        return generateToken(user.getUsername(), user.getRole());
    }

    public String generateToken(String username, UserRole role) {
        Date issuedAt = new Date();
        Date expiryDate = new Date(issuedAt.getTime() + expirationMs);

        return Jwts.builder()
                .issuer(issuer)
                .subject(username)
                .claim(CLAIM_ROLE, role.name())
                .issuedAt(issuedAt)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    public boolean validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        try {
            Claims claims = getClaims(token);
            if (!StringUtils.hasText(claims.getSubject())
                    || claims.getIssuedAt() == null
                    || claims.getExpiration() == null
                    || !claims.getExpiration().after(claims.getIssuedAt())
                    || claims.getIssuedAt().after(new Date())) {
                return false;
            }
            String roleClaim = claims.get(CLAIM_ROLE, String.class);
            return roleClaim != null && UserRole.valueOf(roleClaim) != null;
        } catch (JwtException | IllegalArgumentException | ClassCastException exception) {
            return false;
        }
    }

    public String getUsernameFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public UserRole getRoleFromToken(String token) {
        String roleClaim = getClaims(token).get(CLAIM_ROLE, String.class);
        return UserRole.valueOf(roleClaim);
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
