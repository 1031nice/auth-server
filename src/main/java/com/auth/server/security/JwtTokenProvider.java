package com.auth.server.security;

import com.auth.server.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

  private final JwtConfig jwtConfig;

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
  }

  public String generateAccessToken(String username, Long userId) {
    return buildToken(username, userId, jwtConfig.getExpiration(), "ACCESS");
  }

  public String generateRefreshToken(String username, Long userId) {
    return buildToken(username, userId, jwtConfig.getRefreshExpiration(), "REFRESH");
  }

  private String buildToken(String username, Long userId, Long expirationMillis, String tokenType) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + expirationMillis);

    return Jwts.builder()
        .subject(username)
        .claim("userId", userId)
        .claim("tokenType", tokenType)
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(getSigningKey(), Jwts.SIG.HS512)
        .compact();
  }

  public String getUsernameFromToken(String token) {
    Claims claims = getClaims(token);

    return claims.getSubject();
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      log.error("JWT token validation error: {}", e.getMessage());
      return false;
    }
  }

  public boolean isRefreshToken(String token) {
    return "REFRESH".equals(getClaims(token).get("tokenType", String.class));
  }

  private Claims getClaims(String token) {
    return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
  }
}
