package com.swygbro.airoad.backend.auth.application;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/** JWT 토큰 생성 및 검증을 담당하는 컴포넌트 */
@Slf4j
@Component
public class JwtTokenProvider {

  private final SecretKey secretKey;
  private final long accessTokenValidityInMilliseconds;
  private final long refreshTokenValidityInMilliseconds;

  public JwtTokenProvider(
      @Value("${spring.security.jwt.secret-key}") String secretKey,
      @Value("${spring.security.jwt.access-token-validity}") long accessTokenValidityInMilliseconds,
      @Value("${spring.security.jwt.refresh-token-validity}")
          long refreshTokenValidityInMilliseconds) {
    this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    this.accessTokenValidityInMilliseconds = accessTokenValidityInMilliseconds;
    this.refreshTokenValidityInMilliseconds = refreshTokenValidityInMilliseconds;
  }

  /** Access Token 생성 */
  public String createAccessToken(String email) {
    Date now = new Date();
    Date validity = new Date(now.getTime() + accessTokenValidityInMilliseconds);

    return Jwts.builder()
        .subject(email)
        .issuedAt(now)
        .expiration(validity)
        .signWith(secretKey, Jwts.SIG.HS256)
        .compact();
  }

  /** Refresh Token 생성 */
  public String createRefreshToken(String email) {
    Date now = new Date();
    Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

    return Jwts.builder()
        .subject(email)
        .issuedAt(now)
        .expiration(validity)
        .signWith(secretKey, Jwts.SIG.HS256)
        .compact();
  }

  /** 토큰에서 이메일 추출 */
  public String getEmailFromToken(String token) {
    return getClaims(token).getSubject();
  }

  /** 토큰 유효성 검증 */
  public boolean validateToken(String token) {
    try {
      Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
      return true;
    } catch (SecurityException e) {
      log.warn("JWT 서명이 유효하지 않습니다: {}", e.getMessage());
      return false;
    } catch (MalformedJwtException e) {
      log.warn("JWT 토큰이 올바르지 않습니다: {}", e.getMessage());
      return false;
    } catch (ExpiredJwtException e) {
      log.warn("JWT 토큰이 만료되었습니다: {}", e.getMessage());
      return false;
    } catch (UnsupportedJwtException e) {
      log.warn("지원하지 않는 JWT 토큰입니다: {}", e.getMessage());
      return false;
    } catch (JwtException e) {
      log.error("기타 JWT 토큰 오류: {}", e.getMessage());
      return false;
    } catch (IllegalArgumentException e) {
      log.warn("JWT 토큰이 잘못되었습니다: {}", e.getMessage());
      return false;
    }
  }

  /** Access Token 유효 시간 (초 단위) */
  public long getAccessTokenValidityInSeconds() {
    return accessTokenValidityInMilliseconds / 1000;
  }

  /** Refresh Token 만료 시간 계산 */
  public LocalDateTime getRefreshTokenExpiresAt() {
    return LocalDateTime.now().plusSeconds(refreshTokenValidityInMilliseconds / 1000);
  }

  private Claims getClaims(String token) {
    return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
  }
}
