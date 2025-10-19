package com.swygbro.airoad.backend.auth.filter;

import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.swygbro.airoad.backend.auth.domain.entity.TokenType;
import com.swygbro.airoad.backend.auth.exception.AuthErrorCode;
import com.swygbro.airoad.backend.common.exception.BusinessException;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

  @Value("${jwt.access-token-secret}")
  private String accessTokenSecret;

  @Value("${jwt.refresh-token-secret}")
  private String refreshTokenSecret;

  @Value("${jwt.access-token-expiration}")
  private long accessTokenExpiration;

  @Value("${jwt.refresh-token-expiration}")
  private long refreshTokenExpiration;

  public String generateAccessToken(Long userId, String role) {
    return Jwts.builder()
        .claim("userId", userId)
        .claim("role", role)
        .issuedAt(new Date())
        .expiration(createExpiryDate(accessTokenExpiration))
        .signWith(createKey(accessTokenSecret))
        .compact();
  }

  public String generateRefreshToken(Long userId) {
    return Jwts.builder()
        .claim("userId", userId)
        .claim("type", "REFRESH_TOKEN")
        .issuedAt(new Date())
        .expiration(createExpiryDate(refreshTokenExpiration))
        .signWith(createKey(refreshTokenSecret))
        .compact();
  }

  public <T> T getClaimFromToken(String token, String key, Class<T> clazz, TokenType tokenType) {
    String secretKey =
        (tokenType == TokenType.ACCESS_TOKEN) ? accessTokenSecret : refreshTokenSecret;

    try {
      return Jwts.parser()
          .verifyWith(createKey(secretKey))
          .build()
          .parseSignedClaims(token)
          .getPayload()
          .get(key, clazz);
    } catch (ExpiredJwtException e) {
      log.error("만료된 JWT 토큰입니다.", e);
      throw new BusinessException(AuthErrorCode.EXPIRED_TOKEN);
    } catch (MalformedJwtException e) {
      log.error("잘못된 형식의 JWT 토큰입니다.", e);
      throw new BusinessException(AuthErrorCode.MALFORMED_TOKEN);
    } catch (SecurityException e) {
      log.error("잘못된 JWT 서명입니다.", e);
      throw new BusinessException(AuthErrorCode.INVALID_TOKEN_SIGNATURE);
    } catch (UnsupportedJwtException e) {
      log.error("지원하지 않는 JWT 토큰입니다.", e);
      throw new BusinessException(AuthErrorCode.UNSUPPORTED_TOKEN);
    }
  }

  public void validateAccessToken(String token) {
    validateToken(token, accessTokenSecret);
  }

  public void validateToken(String token, String secretKey) {
    try {
      Jwts.parser().verifyWith(createKey(secretKey)).build().parseSignedClaims(token);
    } catch (ExpiredJwtException e) {
      log.error("만료된 JWT 토큰입니다.", e);
      throw new BusinessException(AuthErrorCode.EXPIRED_TOKEN);
    } catch (MalformedJwtException e) {
      log.error("잘못된 형식의 JWT 토큰입니다.", e);
      throw new BusinessException(AuthErrorCode.MALFORMED_TOKEN);
    } catch (SecurityException e) {
      log.error("잘못된 JWT 서명입니다.", e);
      throw new BusinessException(AuthErrorCode.INVALID_TOKEN_SIGNATURE);
    } catch (UnsupportedJwtException e) {
      log.error("지원하지 않는 JWT 토큰입니다.", e);
      throw new BusinessException(AuthErrorCode.UNSUPPORTED_TOKEN);
    }
  }

  public Date createExpiryDate(long expirationTime) {
    return Date.from(Instant.now().plusMillis(expirationTime));
  }

  public SecretKey createKey(String secret) {
    byte[] keyBytes = Decoders.BASE64URL.decode(secret);
    return Keys.hmacShaKeyFor(keyBytes);
  }
}
