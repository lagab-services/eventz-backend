package com.lagab.eventz.app.domain.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

@Service
public class JwtService {

    @Value("${app.auth.jwt-secret}")
    private String jwtSecret;

    @Value("${app.auth.access-token-expiration:3600}") // in seconds
    private Long accessTokenExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        return Jwts.builder()
                   .claims(extraClaims)
                   .subject(String.valueOf(userId))
                   .issuedAt(Date.from(now))
                   .expiration(Date.from(now.plus(accessTokenExpiration, ChronoUnit.SECONDS)))
                   .signWith(getSigningKey(), Jwts.SIG.HS256)
                   .compact();
    }

    public Claims parseToken(String token) throws JwtException {
        return Jwts.parser()
                   .verifyWith(getSigningKey())
                   .build()
                   .parseSignedClaims(token)
                   .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseToken(token);
            return !claims.getExpiration().before(new Date());
        } catch (SignatureException e) {
            // Invalid signature
            return false;
        } catch (ExpiredJwtException e) {
            // Expired token
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            // Other JWT errors or malformed token
            return false;
        }
    }

    public Long extractUserId(String token) throws JwtException {
        return Long.parseLong(parseToken(token).getSubject());
    }

    public Long getAccessTokenValidityInMilliseconds() {
        return accessTokenExpiration * 1000;
    }
}
