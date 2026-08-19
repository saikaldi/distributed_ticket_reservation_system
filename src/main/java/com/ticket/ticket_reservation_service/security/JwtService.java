package com.ticket.ticket_reservation_service.security;

import com.ticket.ticket_reservation_service.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.HashMap;
import java.util.Map;


import io.jsonwebtoken.Jwts;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;
import java.util.function.Function;


@Service
public class JwtService {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    // Convert the Base64-encoded secret string into a cryptographic HMAC key
    private SecretKey getSignInKey(){
        // 1. Decode Base64 string into raw bytes
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        // 2. Create and return an HMAC-SHA key from the byte array
        return Keys.hmacShaKeyFor(keyBytes);

    }

    // Generate an access token containing user id and role
    public String generateToken(User user) {
        // Prepare additional claims for the token payload
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("userId", user.getId());
        extraClaims.put("role", user.getRole().name());

        // Build and sign the token with expiration time
        return buildToken(extraClaims, user, jwtExpiration);
    }

    // Helper method to configure claims and sign the JWT
    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }

    // Parse and verify the token signature using our signing key
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                // Verify the signature with our secret key
                .verifyWith(getSignInKey())
                // Build the parser
                .build()
                // Parse the token string and return the claims payload
                .parseSignedClaims(token)
                .getPayload();
    }

    // Extract a single claim from the token payload using a resolver function
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // Extract the username (email) from the token's subject claim
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // Check if the token belongs to the given user and is not expired
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    // Verify whether the token expiration date is before the current time
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Extract the expiration timestamp from the token claims
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
