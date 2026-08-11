package com.example.petManagementService.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

// Verification-only counterpart to authService's JwtService. Core never issues tokens —
// only authService does — so there is deliberately no generateToken() here. Validity
// rests entirely on both services sharing the same `jwt.secret`; this must be kept in
// sync with authService's value in every environment.
@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // JJWT/Jackson deserializes small integers as Integer, not Long, so a plain cast to
    // Long.class would throw ClassCastException for ids that happen to fit in an int.
    // Widening any Number defensively avoids that.
    public <T> T extractClaim(String token, String claimKey, Class<T> type) {
        Object raw = extractAllClaims(token).get(claimKey);
        if (raw == null) {
            return null;
        }
        if (type == Long.class && raw instanceof Number number) {
            return type.cast(number.longValue());
        }
        return type.cast(raw);
    }

    // Signature and expiry are checked together: parseSignedClaims() throws for a bad
    // signature or malformed token, and the explicit expiry check below covers a
    // correctly-signed but stale one. Any failure here is "not authenticated", not a 500.
    public boolean isTokenValid(String token) {
        try {
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }
}
