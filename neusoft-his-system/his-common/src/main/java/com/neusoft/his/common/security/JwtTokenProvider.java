package com.neusoft.his.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JwtTokenProvider {
    private final SecretKey key;
    private final long expireSeconds;

    public JwtTokenProvider(String secret, long expireSeconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireSeconds = expireSeconds;
    }

    public String generate(Long userId, String username, Set<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expireSeconds)))
                .signWith(key)
                .compact();
    }

    @SuppressWarnings("unchecked")
    public SecurityUser parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        List<String> roleList = claims.get("roles", List.class);
        return new SecurityUser(Long.parseLong(claims.getSubject()), claims.get("username", String.class),
                roleList == null ? Set.of() : new HashSet<>(roleList));
    }
}
